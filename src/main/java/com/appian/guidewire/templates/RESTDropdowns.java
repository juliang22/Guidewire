package com.appian.guidewire.templates;

import com.appian.connectedsystems.templateframework.sdk.configuration.TextPropertyDescriptor;

public interface RESTDropdowns {

  public TextPropertyDescriptor buildGetDropdown();
  public TextPropertyDescriptor buildPostDropdown();
  public TextPropertyDescriptor buildPatchDropdown();
  public TextPropertyDescriptor buildDeleteDropdown();
}
