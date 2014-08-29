package leshan.client.lwm2m.register;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import leshan.client.lwm2m.LwM2mClient;
import leshan.client.lwm2m.Uplink;
import leshan.client.lwm2m.manage.ManageDownlink;
import leshan.client.lwm2m.response.Callback;
import leshan.client.lwm2m.response.OperationResponse;
import leshan.client.lwm2m.util.LinkFormatUtils;
import leshan.server.lwm2m.client.LinkObject;
import ch.ethz.inf.vs.californium.WebLink;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;

public class RegisterUplink extends Uplink{
	private static final String MESSAGE_NULL_ENDPOINT = "Provided Endpoint was Null";
	private static final String MESSAGE_BAD_OBJECTS = "Objects and Instances Passed Were Not in Valid Link Format.";
	private static final String MESSAGE_BAD_PARAMETERS = "Either the Parameters are Invalid or the Objects and Instances are Null.";
	private static final String ENDPOINT = "ep";
	private final ManageDownlink downlink;
	private final LwM2mClient client;

	public RegisterUplink(final InetSocketAddress destination, final CoAPEndpoint origin, final ManageDownlink downlink, final LwM2mClient client) {
		super(destination, origin);
		if(downlink == null){
			throw new IllegalArgumentException("RegisterDownlink must not be null.");
		}
		this.downlink = downlink;
		this.client = client;
	}

	public OperationResponse register(final String endpointName, final Map<String, String> parameters, final int timeout) {
		if(parameters == null || !areParametersValid(parameters)){
			return OperationResponse.failure(ResponseCode.BAD_REQUEST, MESSAGE_BAD_PARAMETERS);
		}

		final String payload = LinkFormatUtils.payloadize(client.getObjectModel());
		if(payload == null || payload.equals("<>")){
			return OperationResponse.failure(ResponseCode.BAD_REQUEST, MESSAGE_BAD_OBJECTS);
		}
		final ch.ethz.inf.vs.californium.coap.Request request = createRegisterRequest(
				endpointName, payload);
		request.setURI(request.getURI() + "&" + leshan.client.lwm2m.request.Request.toQueryStringMap(parameters));

		return sendSyncRequest(timeout, request);
	}

	public void register(final String endpointName, final Map<String, String> parameters, final Callback callback) {
		if(parameters == null || !areParametersValid(parameters)){
			callback.onFailure(OperationResponse.failure(ResponseCode.BAD_REQUEST, MESSAGE_BAD_PARAMETERS));
			return;
		}

		final String payload = LinkFormatUtils.payloadize(client.getObjectModel());
		if(payload == null || payload.equals("<>")){
			callback.onFailure(OperationResponse.failure(ResponseCode.BAD_REQUEST, MESSAGE_BAD_OBJECTS));
			return;
		}

		final ch.ethz.inf.vs.californium.coap.Request request = createRegisterRequest(
				endpointName, payload);
		request.setURI(request.getURI() + "&" + leshan.client.lwm2m.request.Request.toQueryStringMap(parameters));

		sendAsyncRequest(callback, request);
	}

	public void delete(final String location, final Callback callback) {
		// TODO Auto-generated method stub

	}

	public void update(final String endpointLocation, final Map<String, String> parameters, final Callback callback) {
		if(parameters == null || !areParametersValid(parameters) || parameters.isEmpty()){
			callback.onFailure(OperationResponse.failure(ResponseCode.BAD_REQUEST, MESSAGE_BAD_PARAMETERS));
			return;
		}

		final String payload = LinkFormatUtils.payloadize(client.getObjectModel());

		final Request request = createUpdateRequest(endpointLocation, parameters);
		if(!payload.equals("<>")){
			request.setPayload(payload);
		}

		sendAsyncRequest(callback, request);
	}

	public OperationResponse update(final String endpointLocation, final Map<String, String> parameters, final long timeout) {
		if(parameters == null || !areParametersValid(parameters) || parameters.isEmpty()){
			return OperationResponse.failure(ResponseCode.BAD_REQUEST, MESSAGE_BAD_PARAMETERS);
		}
		
		final String payload = LinkFormatUtils.payloadize(client.getObjectModel());

		final Request request = createUpdateRequest(endpointLocation, parameters);
		if(!payload.equals("<>")){
			request.setPayload(payload);
		}

		return sendSyncRequest(timeout, request);
	}

	public OperationResponse deregister(final String endpointLocation, final long timeout) {
		if(endpointLocation == null){
			return OperationResponse.failure(ResponseCode.NOT_FOUND, MESSAGE_NULL_ENDPOINT);
		}

		final ch.ethz.inf.vs.californium.coap.Request request = createDeregisterRequest(endpointLocation);

		final OperationResponse response = sendSyncRequest(timeout, request);

		origin.stop();

		return response;
	}

	public void deregister(final String endpointLocation, final Callback callback) {
		if(endpointLocation == null){
			callback.onFailure(OperationResponse.failure(ResponseCode.NOT_FOUND, MESSAGE_NULL_ENDPOINT));
		}

		final ch.ethz.inf.vs.californium.coap.Request request = createDeregisterRequest(endpointLocation);

		sendAsyncRequest(new Callback(){
			final Callback initializingCallback = callback;

			@Override
			public void onSuccess(final OperationResponse response) {
				initializingCallback.onSuccess(response);
				origin.stop();
			}

			@Override
			public void onFailure(final OperationResponse response) {
				initializingCallback.onFailure(response);
				origin.stop();
			}

		}, request);
	}

	public OperationResponse notify(final String todo) {
		return null;
	}

	private ch.ethz.inf.vs.californium.coap.Request createRegisterRequest(
			final String endpointName, final String payload) {
		final ch.ethz.inf.vs.californium.coap.Request request = ch.ethz.inf.vs.californium.coap.Request.newPost();
		final RegisterEndpoint registerEndpoint = new RegisterEndpoint(getDestination(), Collections.singletonMap(ENDPOINT, endpointName));
		request.setURI(registerEndpoint.toString());
		request.setPayload(payload);
		return request;
	}

	private Request createUpdateRequest(final String endpointLocation,
			final Map<String, String> parameters) {
		final Request request = Request.newPut();
		final RegisteredEndpoint registerEndpoint = new RegisteredEndpoint(getDestination(), endpointLocation);
		request.setURI(registerEndpoint.toString());
		request.getOptions().setURIQuery(leshan.client.lwm2m.request.Request.toQueryStringMap(parameters));

		return request;
	}

	private ch.ethz.inf.vs.californium.coap.Request createDeregisterRequest(
			final String endpointLocation) {
		final ch.ethz.inf.vs.californium.coap.Request request = ch.ethz.inf.vs.californium.coap.Request.newDelete();
		final RegisteredEndpoint deregisterEndpoint = new RegisteredEndpoint(getDestination(), endpointLocation);
		request.getOptions().setLocationPath(endpointLocation);
		request.setURI(deregisterEndpoint.toString());

		return request;
	}

	private boolean areParametersValid(final Map<String, String> parameters) {
		for(final Map.Entry<String, String> p : parameters.entrySet()){
			switch(p.getKey()){
			case "lt" :
				break;
			case "lwm2m" :
				break;
			case "sms" :
				return false;
			case "b" :
				if(!isBindingValid(p.getValue())){
					return false;
				}
				break;
			default:
				return false;
			}
		}

		return true;
	}

	private boolean isBindingValid(final String value) {
		if(value.equals("U")){
			return true;
		}

		return false;
	}

}
