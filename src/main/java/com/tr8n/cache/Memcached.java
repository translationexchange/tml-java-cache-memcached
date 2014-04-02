/*
 *  Copyright (c) 2014 Michael Berkovich, http://tr8nhub.com All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.tr8n.cache;

import java.util.Map;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import com.tr8n.core.Cache;
import com.tr8n.core.Tr8n;

public class Memcached extends Cache {
	MemcachedClient client;
	Integer version;
	
	public Memcached(Map<String, Object> config) {
		super(config);
	}

	private MemcachedClient getMemcachedClient() throws Exception {
		if (client == null) {
			client = new MemcachedClient(AddrUtil.getAddresses((String)getConfig().get("host")));
		}
		
		return client;
	}

	public Integer getVersion() {
		try {
			version = (Integer) getMemcachedClient().get("version");
			if (version == null) {
				version = (Integer) getConfig().get("version");
				setVersion(version);
			}
		} catch (Exception ex) {
			version = (Integer) getConfig().get("version");
		}
		
		return version;
	}
	
	public void setVersion(Integer version) {
		try {
			getMemcachedClient().set("version", 0, version);
			this.version = version;
		} catch (Exception ex) {
		}
	}

	public void incrementVersion() {
		setVersion(getVersion() + 1);
	}
	
	protected String getVersionedKey(String key) {
		return getVersion() + "_" + key;
	}

	private int getTimeout() {
		if (getConfig().get("timeout") == null) 
			return 0;
		return (Integer) getConfig().get("timeout");
	}
	
	@Override
	public Object fetch(String key, Map<String, Object> options) {
		if (isInlineMode(options)) return null;
		
		try {
			return getMemcachedClient().get(getVersionedKey(key));
		} catch (Exception ex) {
			Tr8n.getLogger().logException("Failed to get a value from Memcached", ex);
			return null;
		}
	}

	@Override
	public void store(String key, Object data, Map<String, Object> options) {
		if (isInlineMode(options)) return;

		try {
			getMemcachedClient().set(getVersionedKey(key), getTimeout(), data);
		} catch (Exception ex) {
			Tr8n.getLogger().logException("Failed to store a value in Memcached", ex);
		}
	}

	@Override
	public void delete(String key, Map<String, Object> options) {
		try {
			getMemcachedClient().delete(getVersionedKey(key));
		} catch (Exception ex) {
			Tr8n.getLogger().logException("Failed to delete a value from Memcached", ex);
		}
	}

}
