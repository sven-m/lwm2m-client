package com.itude.mobile.oma.lwm2m.tool;

import java.util.Scanner;

import com.itude.mobile.oma.lwm2m.client.Lwm2mClient;

public class Main {
	public static void main(final String[] args) throws Exception {

		final String host = args[0];
		final int port = Integer.parseInt(args[1]);
		final String endpointName = args[2];
		final String objectValue = args[3];
		final String resourceValue = args[4];

		final Lwm2mClient client = new Lwm2mClient(host, port, endpointName,
				objectValue, resourceValue);

		final boolean registered = client.register();

		if (registered) {
			addShutDownHook(client);
		}

		final Scanner userInput = new Scanner(System.in);
		while (true) {
			if (userInput.hasNextLine()) {
				final String line = userInput.nextLine();
				if (line.equals("update")) {
					if (client.update()) {
						System.out.println("successfully updated registration");
					}
				} else if (line.equals("exit")) {
					break;
				} else {
					System.out
							.println("Unknown command: use \"exit\" or \"update\"");
				}
			}
		}
		userInput.close();
		client.stop();
		System.exit(0);
	}

	private static void addShutDownHook(final Lwm2mClient client) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("shutting down");
				try {
					client.deregister();
				} catch (final Exception e) {
					System.out.println(e.getMessage());
				}

			}
		});
		;
	}
}
