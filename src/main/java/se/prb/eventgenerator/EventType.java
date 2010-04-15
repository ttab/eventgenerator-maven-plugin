package se.prb.eventgenerator;

public enum EventType {
	event("GwtEvent", null), request("AbstractRequest", "Response"), response("AbstractResponse", "Request");

	public final String defaultSuperClass;
	public final String partnerClassSuffix;
	
	private EventType(String defaultSuperClass, String partnerClassSuffix) {
		this.defaultSuperClass = defaultSuperClass;
		this.partnerClassSuffix = partnerClassSuffix;
	}
}