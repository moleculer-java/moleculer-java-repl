package testcase;

import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.Group;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Dependencies;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("math")
@Dependencies({"loggerService", "storageService"})
public class MathService extends Service {

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		
		// User-defined init method
	}

	@Cache(keys = { "a", "b" }, ttl = 60000)
	public Action add = ctx -> {

		// Body of the distributed method ("action")
		return ctx.params.get("a").asInteger()
			 + ctx.params.get("b").asInteger();

	};

	@Subscribe("user.created")
	@Group("optionalEventGroup")
	public Listener userCreated = payload -> {
		
		// Body of the distributed event listener method
		System.out.println("Received: " + payload);
	};
	
	@Override
	public void stopped() {
		
		// User-defined destroy method
	}

}
