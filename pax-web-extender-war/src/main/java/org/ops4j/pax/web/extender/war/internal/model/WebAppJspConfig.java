package org.ops4j.pax.web.extender.war.internal.model;

import java.util.ArrayList;
import java.util.List;

public class WebAppJspConfig {
	
	List<WebAppTagLib> tagLibConfigs = new ArrayList<>();
	List<WebAppJspPropertyGroup> jspPropertyGroups = new ArrayList<>();
	
	public void addTagLibConfig(WebAppTagLib tagLibConfig) {
		tagLibConfigs.add(tagLibConfig);
	}
	
	public void addJspPropertyGroup(WebAppJspPropertyGroup jspPropertyGroup) {
		jspPropertyGroups.add(jspPropertyGroup);
	}
	
	public List<WebAppJspPropertyGroup> getJspPropertyGroups() {
		return jspPropertyGroups;
	}
	
	public List<WebAppTagLib> getTagLibConfigs() {
		return tagLibConfigs;
	}
}
