package services.moleculer.repl;

import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cache;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.Version;

public class Sample {

	public static void main(String[] args) throws Exception {
		System.out.println("START");
		try {
			ServiceBrokerConfig cfg = new ServiceBrokerConfig();
			
			// RedisTransporter t = new RedisTransporter();
			// t.setDebug(false);
			// cfg.setTransporter(t);
							
			ServiceBroker broker = new ServiceBroker(cfg);
	
			broker.createService(new Service("math") {

				@Name("add")
				@Cache(keys = { "a", "b" }, ttl = 30)
				public Action add = ctx -> {

					//broker.getLogger().info("Call " + ctx.params);
					return ctx.params.get("a", 0) + ctx.params.get("b", 0);

				};

				@Name("test")
				@Version("1")
				public Action test = ctx -> {

					return ctx.params.get("a", 0) + ctx.params.get("b", 0);

				};

				@Subscribe("foo.*")
				public Listener listener = payload -> {
					System.out.println("Received: " + payload);
				};

			});			

			broker.start();	
			broker.repl();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("STOP");
	}

}