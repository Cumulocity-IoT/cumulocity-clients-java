/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * ATM Software MediaPortal 2.0
 *
 * Copyright 2010 - 2011 ATM Software
 * Author: D. Kaczyński
 */
package com.cumulocity.me.http.impl;

import org.eclipse.jetty.server.Server;

public class TestServerRunner implements Runnable {

	private Server server;
	private AfterStartCallback callback;
	
	public TestServerRunner(Server server) {
		this(server,  null);
	}
	
	public TestServerRunner(Server server, AfterStartCallback callback) {
		this.server = server;
		this.callback = callback;
	}
	
	/** {@inheritDoc} */
	@Override
	public void run() {
		try {
			server.start();
			if (callback != null) {
				callback.execute();
			}
			server.join();
		} catch (Exception e) {
			throw new RuntimeException("Error running server!", e);
		}
	}
	
	public Server getServer() {
		return server;
	}
	
	public static interface AfterStartCallback {
		
		void execute() throws Exception;
	}
}
