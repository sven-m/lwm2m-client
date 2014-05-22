package com.itude.mobile.oma.lwm2m.client;

import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.OptionSet;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class Lwm2mClient {
	private final String mgmtServerHost;
	private final int mgmtServerPort;
	private final String clientEndpointName;
	private final String objectValue;
	private final String resourceValue;

	private State state;

	private Server server;
	private String mgmtServerPath;
	private Endpoint endpoint;

	public Lwm2mClient(final String host, final int port,
			final String endpointName, final String objectValue,
			final String resourceValue) {

		this.mgmtServerHost = host;
		this.mgmtServerPort = port;
		this.clientEndpointName = endpointName;
		this.objectValue = objectValue;
		this.resourceValue = resourceValue;
	}

	public Server getServer() {
		return this.server;
	}

	private void setServer(final Server server) {
		this.server = server;
	}

	public String getMgmtServerPath() {
		return this.mgmtServerPath;
	}

	private void setMgmtServerPath(final String mgmtServerPath) {
		this.mgmtServerPath = mgmtServerPath;
	}

	public Endpoint getEndpoint() {
		if (this.endpoint == null) {
			this.endpoint = listen();
		}

		return this.endpoint;
	}

	public String getMgmtServerHost() {
		return this.mgmtServerHost;
	}

	public int getMgmtServerPort() {
		return this.mgmtServerPort;
	}

	public String getClientEndpointName() {
		return this.clientEndpointName;
	}

	public String getObjectValue() {
		return this.objectValue;
	}

	public String getResourceValue() {
		return this.resourceValue;
	}

	public State getState() {
		return this.state;
	}

	private void setState(final State state) {
		this.state = state;
	}

	private Endpoint listen() {

		setServer(new Server());

		final Resource topResource = new ResourceBase("3") {
			@Override
			public void handleGET(final CoapExchange exchange) {
				exchange.respond(ResponseCode.CONTENT, "main object value");
			}

			@Override
			public String getPath() {
				return "/";
			}
		};

		final Resource instanceResource = new ResourceBase("1") {
			@Override
			public void handleGET(final CoapExchange exchange) {
				exchange.respond(ResponseCode.CONTENT, getObjectValue());
			}

			@Override
			public String getPath() {
				return "/3/";
			}
		};

		topResource.add(instanceResource);

		instanceResource.add(new ResourceBase("0") {
			@Override
			public void handleGET(final CoapExchange exchange) {
				exchange.respond(ResponseCode.CONTENT, getResourceValue());
			}

			@Override
			public String getPath() {
				return "/3/1/";
			}
		});

		getServer().add(topResource);

		getServer().start();

		return getServer().getEndpoints().get(0);
	}

	public boolean register() throws AlreadyRegisteredException {
		if (getState() == State.REGISTERED) {
			throw this.new AlreadyRegisteredException(
					"Client is already registered to server: "
							+ this.mgmtServerHost);
		} else {
			final boolean success = doRegister();

			if (success) {
				setState(State.REGISTERED);
			} else {
				setState(State.ERROR);
			}

			return success;
		}
	}

	private boolean doRegister() {
		boolean success = false;

		final Request request = new Request(Code.POST);

		final String uri = "coap://" + getMgmtServerHost() + ":"
				+ getMgmtServerPort() + "/proxy";

		final String proxyUri = "coap://" + getMgmtServerHost() + ":"
				+ getMgmtServerPort() + "/rd?ep=" + getClientEndpointName()
				+ "&lt=10&b=U";
		request.setURI(uri);
		request.getOptions().setProxyURI(proxyUri);

		request.setPayload("</3/1>");
		getEndpoint().sendRequest(request);

		Response response;
		ResponseCode responseCode = null;

		try {
			response = request.waitForResponse();
		} catch (final InterruptedException e) {
			System.out.println(e.getMessage());
			return false;
		}

		responseCode = response.getCode();
		success = (responseCode == CoAP.ResponseCode.CREATED);

		if (success) {
			final OptionSet options = response.getOptions();
			setMgmtServerPath(options.getLocationPaths().get(1));
		}

		return success;
	}

	public boolean update() throws NotYetRegisteredException {
		if (getState() == State.UNREGISTERED) {
			throw this.new NotYetRegisteredException(
					"Client needs to be registered with a server in order to execute an update");
		} else {
			final boolean success = doUpdate();

			if (!success) {
				setState(State.ERROR);
			}

			return success;
		}
	}

	private boolean doUpdate() {
		boolean success = false;

		final Request request = new Request(Code.PUT);
		request.setURI(this.mgmtServerHost + ":" + this.mgmtServerPort + "/rd/"
				+ this.mgmtServerPath);

		getEndpoint().sendRequest(request);

		Response response;
		ResponseCode responseCode = null;

		try {
			response = request.waitForResponse();
		} catch (final InterruptedException e) {
			System.out.println(e.getMessage());
			return false;
		}

		responseCode = response.getCode();
		success = (responseCode == CoAP.ResponseCode.CHANGED);

		return success;
	}

	public boolean deregister() throws NotYetRegisteredException {
		if (getState() == State.UNREGISTERED) {
			throw this.new NotYetRegisteredException(
					"Client needs to be registered with a server in order to deregister");
		} else {
			final boolean success = doDeregister();

			if (success) {
				setState(State.UNREGISTERED);
			} else {
				setState(State.ERROR);
			}

			return success;
		}
	}

	private boolean doDeregister() {
		boolean success = false;

		final Request request = new Request(Code.DELETE);
		request.setURI(this.mgmtServerHost + ":" + this.mgmtServerPort + "/rd/"
				+ this.mgmtServerPath);

		getEndpoint().sendRequest(request);

		Response response;
		ResponseCode responseCode = null;

		try {
			response = request.waitForResponse();
		} catch (final InterruptedException e) {
			System.out.println(e.getMessage());
			return false;
		}

		responseCode = response.getCode();
		success = (responseCode == CoAP.ResponseCode.DELETED);

		return success;
	}

	public void stop() {
		if (getState() == State.REGISTERED) {
			doDeregister();
		}

		getServer().destroy();
		setServer(null);
	}

	public static enum State {
		REGISTERED, UNREGISTERED, ERROR;
	}

	public class AlreadyRegisteredException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -5686254328677164602L;

		public AlreadyRegisteredException(final String message) {
			super(message);
		}
	}

	public class NotYetRegisteredException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2243219445786347002L;

		public NotYetRegisteredException(final String message) {
			super(message);
		}
	}

}
