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
package services.moleculer.repl;

import services.moleculer.ServiceBroker;
import services.moleculer.breaker.CircuitBreaker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.transporter.RedisTransporter;
import services.moleculer.transporter.Transporter;

public class Sample {

	public static void main(String[] args) throws Exception {
		try {
			
			// Create Service Broker config
			ServiceBrokerConfig cfg = new ServiceBrokerConfig();

			// Unique nodeID
			cfg.setNodeID("node1");

			// Define a brokerless transporter
			// TcpTransporter t = new TcpTransporter();
			// t.setUseHostname(false);
			// t.setSerializer(new MsgPackSerializer());
			Transporter t = new RedisTransporter("192.168.51.100");
			
			cfg.setTransporter(t);
			
			CircuitBreaker cb = new CircuitBreaker();
			// cb.setMaxErrors(1);
			// cb.setLockTimeout(5000);
			cfg.setServiceInvoker(cb);
			
			// DefaultServiceInvoker di = new DefaultServiceInvoker();
			// di.setWriteErrorsToLog(false);
			// cfg.setServiceInvoker(di);
			
			// Create Service Broker (by config)
			ServiceBroker broker = new ServiceBroker(cfg);

			// Start Service Broker
			broker.start();

			// Create local REPL developer console
			LocalRepl repl = new LocalRepl();

			// Load custom (user-defined) commands
			repl.setPackagesToScan("testcase");

			// Install REPL service (REPL console is a Moleculer Service)
			broker.createService("$repl", repl);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}