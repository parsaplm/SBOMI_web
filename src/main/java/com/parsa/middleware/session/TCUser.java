package com.parsa.middleware.session;

import java.io.IOException;

public class TCUser {
	public String tcUserId;
	public String tcServerUrl;
	public String tcPassword;
	public String tcGroup;

	public TCUser() {

	}

	public TCUser(String tcUserId, String tcServerId, String tcPassword, String tcGroup) throws IOException {
		this.tcUserId = tcUserId;
		tcServerUrl = tcServerId;
		this.tcPassword = tcPassword;
		this.tcGroup = tcGroup;

	}

}
