package detectors;

public abstract class StartStoppable {

//	Thread loop = new Thread(() -> {
//		while (activated) {
//			loop();
//		}
//	});

	public final void start() {
		begin();
//		loop.start();
	}

	public abstract void begin();

	public final void stop() {
		end();
		//yes stop() is deprecated but there is no big penalty for premature death
		//and suspend is useless as well because each cycle is independent
//		loop.interrupt();
	}

	public abstract void end();

	public abstract void loop();
}
