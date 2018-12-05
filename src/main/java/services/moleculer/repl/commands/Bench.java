/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.repl.commands;

import static services.moleculer.repl.ColorWriter.GRAY;
import static services.moleculer.repl.ColorWriter.GREEN;
import static services.moleculer.repl.ColorWriter.WHITE;
import static services.moleculer.repl.ColorWriter.YELLOW;
import static services.moleculer.util.CommonUtils.formatNamoSec;
import static services.moleculer.util.CommonUtils.formatNumber;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallOptions;
import services.moleculer.error.ServiceNotAvailableError;
import services.moleculer.error.ServiceNotFoundError;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;

/**
 * Measures the response time of a service.
 */
@Name("bench")
public class Bench extends Command {

	public Bench() {
		option("num <number>", "number of iterates");
		option("time <seconds>", "time of bench");
		option("nodeID <nodeID>", "nodeID (direct call)");
		option("max <number>", "max number of pending requests");
		option("retry <number>", "max number of retries (default is 0)");		
	}

	@Override
	public String getDescription() {
		return "Benchmark a service";
	}

	@Override
	public String getUsage() {
		return "bench <action> [jsonParams]";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 1;
	}

	protected ScheduledFuture<?> timer;

	protected ExecutorService executor;

	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {
		executor = broker.getConfig().getExecutor();

		// Check parameter sequence
		if (parameters[0].startsWith("--")) {
			out.println("Invalid parameter sequence! Examples of appropriate \"bench\" commands:");
			out.println();
			out.println("bench $node.list --num 100");
			out.println("bench $node.list --num 100 --nodeID node1");
			out.println("bench $node.list --time 10");
			out.println("bench $node.list --time 10 --max 10");
			out.println("bench $node.actions --time 120 {\"onlyLocal\":true}");
			out.println("bench $node.actions --time 120 onlyLocal true");
			return;
		}
		
		// Parse parameters
		String action = parameters[0];
		Collection<String> knownParams = Arrays.asList(new String[]{"num", "time", "nodeID", "max", "retry"});
		Tree flags = parseFlags(1, parameters, knownParams);
		long num = flags.get("num", 0);
		long time = flags.get("time", 0);
		int retry = flags.get("retry", 0);
		String nodeID = flags.get("nodeID", "");
		int lastIndex = flags.get("lastIndex", 0);
		int max = flags.get("max", 100);
		if (num > 0) {
			max = Math.min(max, (int) num);
		}
		Tree params = getPayload(lastIndex + 1, parameters);

		if (num < 1 && time < 1) {
			time = 5;
		}
		if (max < 1) {
			max = 1;
		}
		CallOptions.Options opts = CallOptions.retryCount(retry);
		if (nodeID != null && !nodeID.isEmpty()) {
			opts = opts.nodeID(nodeID);
		}

		// Start timer
		BenchData data = new BenchData(broker, opts, out, action, params, num);
		if (timer != null) {
			timer.cancel(true);
		}
		timer = broker.getConfig().getScheduler().schedule(() -> {
			data.timeout.set(true);
		}, time < 1 ? 60 : time, TimeUnit.SECONDS);

		// Start benchmark...
		String msg = num > 0 ? num + " times" : "for " + formatNamoSec(time * 1000000000);
		out.println(YELLOW + ">> Calling '" + action + "' " + msg + " with params: "
				+ params.toString("colorized-json", false));
		out.println();

		long req, res;
		while (!data.finished.get()) {
			req = data.reqCount.get();
			res = data.resCount.get();
			if (req - res < max) {
				doRequest(broker, data);
			} else {
				Thread.sleep(1);
			}
		}
		
		// Waiting for printing report
		Thread.sleep(100);
	}

	protected void doRequest(ServiceBroker broker, BenchData data) {
		data.reqCount.incrementAndGet();
		long startTime = System.nanoTime();
		try {
			broker.call(data.action, data.params, data.opts).then(res -> {
				handleResponse(broker, data, startTime, null);
			}).catchError(cause -> {
				handleResponse(broker, data, startTime, cause);
			});			
		} catch (Exception err) {
			handleResponse(broker, data, startTime, err);
		}
	}

	protected void handleResponse(ServiceBroker broker, BenchData data, long startTime, Throwable cause) {
		if (data.finished.get()) {
			return;
		}

		long duration = System.nanoTime() - startTime;
		data.sumTime.addAndGet(duration);
	
		long count = data.resCount.incrementAndGet();
		if (cause != null) {
			if (data.errorCount.incrementAndGet() == 1) {
				data.cause = cause;
			}
			Throwable type;
			if (cause instanceof CompletionException) {
				type = ((CompletionException) cause).getCause();
			} else {
				type = cause;
			}
			if (type instanceof ServiceNotFoundError || type instanceof ServiceNotAvailableError) {
				if (data.finished.compareAndSet(false, true)) {
					if (timer != null) {
						timer.cancel(true);
					}
				}
				return;
			}
		}

		long currentMin = data.minTime.get();
		while (true) {
			if (duration < currentMin) {
				if (data.minTime.compareAndSet(currentMin, duration)) {
					break;
				}
				currentMin = data.minTime.get();
			} else {
				break;
			}
		}

		long currentMax = data.maxTime.get();
		while (true) {
			if (duration > currentMax) {
				if (data.maxTime.compareAndSet(currentMax, duration)) {
					break;
				}
				currentMax = data.maxTime.get();
			} else {
				break;
			}
		}

		if (data.timeout.get() || (data.num > 0 && count >= data.num)) {
			if (data.finished.compareAndSet(false, true)) {
				if (timer != null) {
					timer.cancel(true);
				}
				printResult(data);
			}
			return;
		}

		if (count % 100 > 0) {
			doRequest(broker, data);
		} else {
			executor.execute(() -> {
				doRequest(broker, data);
			});
		}
	}

	protected void printResult(BenchData data) {
		PrintWriter out = data.out;
		try {
			long now = System.nanoTime();

			BigDecimal errorCount = new BigDecimal(data.errorCount.get());
			BigDecimal resCount = new BigDecimal(data.resCount.get());
			BigDecimal sumTime = new BigDecimal(data.sumTime.get());

			long total = now - data.startTime;
			BigDecimal totalTime = new BigDecimal(total);

			BigDecimal nano = new BigDecimal(1000000000);
			BigDecimal reqPerSec = nano.multiply(resCount).divide(totalTime, RoundingMode.HALF_UP);
			long reqPer = Long.parseLong(reqPerSec.toBigInteger().toString());

			BigDecimal duration = sumTime.divide(resCount, RoundingMode.HALF_UP);
			long dur = Long.parseLong(duration.toBigInteger().toString());
			BigDecimal inSec = duration.divide(nano);

			String errStr;
			if (errorCount.compareTo(BigDecimal.ZERO) == 1) {
				String percent = errorCount.multiply(new BigDecimal(100)).divide(resCount, RoundingMode.HALF_UP)
						.toBigInteger().toString();
				errStr = formatNumber(data.errorCount) + " error(s) " + percent + "%";
			} else {
				errStr = "0 error";
			}
			out.println(GREEN + "Benchmark results:");
			out.println();
			out.println("  " + WHITE + formatNumber(data.resCount) + " requests in " + formatNamoSec(total) + ", "
					+ GRAY + errStr);
			out.println();
			out.println("  Requests per second: " + WHITE + formatNumber(reqPer));
			out.println();
			out.println("  Latency: ");
			out.println("    Average: " + WHITE + formatNamoSec(dur) + " (" + inSec.toPlainString() + " second)");
			if (data.minTime.get() != Long.MAX_VALUE) {
				out.println("    Minimum: " + WHITE + formatNamoSec(data.minTime.get()));
			}
			if (data.maxTime.get() != Long.MIN_VALUE) {
				out.println("    Maximum: " + WHITE + formatNamoSec(data.maxTime.get()));
			}
			if (data.cause != null) {
				out.println();
				out.println(YELLOW + "Trace of the first faulty response:");
				out.println();
				data.cause.printStackTrace(out);
			}
		} catch (Exception e) {
			e.printStackTrace(out);
		}
	}

	protected static final class BenchData {

		protected final long startTime;

		protected final ServiceBroker broker;
		protected final CallOptions.Options opts;
		protected final PrintWriter out;
		protected final String action;
		protected final Tree params;
		protected final long num;

		protected final AtomicLong reqCount = new AtomicLong();
		protected final AtomicLong resCount = new AtomicLong();
		protected final AtomicLong errorCount = new AtomicLong();
		protected final AtomicLong sumTime = new AtomicLong();

		protected final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
		protected final AtomicLong maxTime = new AtomicLong(Long.MIN_VALUE);

		protected final AtomicBoolean timeout = new AtomicBoolean();
		protected final AtomicBoolean finished = new AtomicBoolean();

		protected Throwable cause;
		
		protected BenchData(ServiceBroker broker, CallOptions.Options opts, PrintWriter out, String action, Tree params,
				long num) {
			this.broker = broker;
			this.opts = opts;
			this.out = out;
			this.action = action;
			this.params = params;
			this.num = num;
			this.startTime = System.nanoTime();
		}

	}

}