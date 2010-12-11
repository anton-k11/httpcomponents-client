/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.http.client.cache;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;

public class TestHttpCacheEntry {

	private Date now;
	private Date elevenSecondsAgo;
	private Date nineSecondsAgo;
	private Resource mockResource;
	private StatusLine statusLine;
	private HttpCacheEntry entry;

	@Before
	public void setUp() {
		now = new Date();
		elevenSecondsAgo = new Date(now.getTime() - 11 * 1000L);
		nineSecondsAgo = new Date(now.getTime() - 9 * 1000L);
		statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1,
				HttpStatus.SC_OK, "OK");
		mockResource = createMock(Resource.class);
	}

	private HttpCacheEntry makeEntry(Header[] headers) {
		return new HttpCacheEntry(elevenSecondsAgo, nineSecondsAgo,
				statusLine, headers, mockResource, null);
	}

	@Test
	public void testGetHeadersReturnsCorrectHeaders() {
		Header[] headers = { new BasicHeader("foo", "fooValue"),
				new BasicHeader("bar", "barValue1"),
				new BasicHeader("bar", "barValue2")
		};
		entry = makeEntry(headers);
		assertEquals(2, entry.getHeaders("bar").length);
	}

	@Test
	public void testGetFirstHeaderReturnsCorrectHeader() {
		Header[] headers = { new BasicHeader("foo", "fooValue"),
				new BasicHeader("bar", "barValue1"),
				new BasicHeader("bar", "barValue2")
		};
		entry = makeEntry(headers);
		assertEquals("barValue1", entry.getFirstHeader("bar").getValue());
	}

	@Test
	public void testGetHeadersReturnsEmptyArrayIfNoneMatch() {
		Header[] headers = { new BasicHeader("foo", "fooValue"),
				new BasicHeader("bar", "barValue1"),
				new BasicHeader("bar", "barValue2")
		};
		entry = makeEntry(headers);
		assertEquals(0, entry.getHeaders("baz").length);
	}

	@Test
	public void testGetFirstHeaderReturnsNullIfNoneMatch() {
		Header[] headers = { new BasicHeader("foo", "fooValue"),
				new BasicHeader("bar", "barValue1"),
				new BasicHeader("bar", "barValue2")
		};
		entry = makeEntry(headers);
		assertEquals(null, entry.getFirstHeader("quux"));
	}

	@Test
	public void testCacheEntryWithNoVaryHeaderDoesNotHaveVariants() {
		Header[] headers = new Header[0];
		entry = makeEntry(headers);
		assertFalse(entry.hasVariants());
	}

	@Test
	public void testCacheEntryWithOneVaryHeaderHasVariants() {
		Header[] headers = { new BasicHeader("Vary", "User-Agent") };
		entry = makeEntry(headers);
		assertTrue(entry.hasVariants());
	}

	@Test
	public void testCacheEntryWithMultipleVaryHeadersHasVariants() {
		Header[] headers = { new BasicHeader("Vary", "User-Agent"),
				new BasicHeader("Vary", "Accept-Encoding")
		};
		entry = makeEntry(headers);
		assertTrue(entry.hasVariants());
	}

	@Test
	public void testCacheEntryWithVaryStarHasVariants(){
		Header[] headers = { new BasicHeader("Vary", "*") };
		entry = makeEntry(headers);
		assertTrue(entry.hasVariants());
	}

	@Test
	public void mustProvideRequestDate() {
		try {
			new HttpCacheEntry(null, new Date(), statusLine,
					new Header[]{}, mockResource, null);
			fail("Should have thrown exception");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void mustProvideResponseDate() {
		try {
			new HttpCacheEntry(new Date(), null, statusLine,
					new Header[]{}, mockResource, null);
			fail("Should have thrown exception");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void mustProvideStatusLine() {
		try {
			new HttpCacheEntry(new Date(), new Date(), null,
					new Header[]{}, mockResource, null);
			fail("Should have thrown exception");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void mustProvideResponseHeaders() {
		try {
			new HttpCacheEntry(new Date(), new Date(), statusLine,
					null, mockResource, null);
			fail("Should have thrown exception");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void mustProvideResource() {
		try {
			new HttpCacheEntry(new Date(), new Date(), statusLine,
					new Header[]{}, null, null);
			fail("Should have thrown exception");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void canProvideVariantSet() {
		new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, new HashSet<String>());
	}

	@Test
	public void canRetrieveOriginalStatusLine() {
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, null);
		assertSame(statusLine, entry.getStatusLine());
	}

	@Test
	public void protocolVersionComesFromOriginalStatusLine() {
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, null);
		assertSame(statusLine.getProtocolVersion(),
				entry.getProtocolVersion());    	
	}

	@Test
	public void reasonPhraseComesFromOriginalStatusLine() {
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, null);
		assertSame(statusLine.getReasonPhrase(), entry.getReasonPhrase());    	
	}

	@Test
	public void statusCodeComesFromOriginalStatusLine() {
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, null);
		assertEquals(statusLine.getStatusCode(), entry.getStatusCode());    	
	}

	@Test
	public void canGetOriginalRequestDate() {
		final Date requestDate = new Date();
		entry = new HttpCacheEntry(requestDate, new Date(), statusLine,
				new Header[]{}, mockResource, null);
		assertSame(requestDate, entry.getRequestDate());    	
	}

	@Test
	public void canGetOriginalResponseDate() {
		final Date responseDate = new Date();
		entry = new HttpCacheEntry(new Date(), responseDate, statusLine,
				new Header[]{}, mockResource, null);
		assertSame(responseDate, entry.getResponseDate());    	
	}

	@Test
	public void canGetOriginalResource() {
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, null);
		assertSame(mockResource, entry.getResource());    	
	}

	@Test
	public void canGetOriginalHeaders() {
		Header[] headers = {
				new BasicHeader("Server", "MockServer/1.0"),
				new BasicHeader("Date", DateUtils.formatDate(now))
		};
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				headers, mockResource, null);
		Header[] result = entry.getAllHeaders();
		assertEquals(headers.length, result.length);
		for(int i=0; i<headers.length; i++) {
			assertEquals(headers[i], result[i]);
		}
	}

	@Test
	public void canRetrieveOriginalVariantSet() {
		Set<String> variants = new HashSet<String>();
		variants.add("variant1");
		variants.add("variant2");
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, variants);
		Set<String> result = entry.getVariantURIs();
		assertEquals(variants.size(), result.size());
		for(String variant : variants) {
			assertTrue(result.contains(variant));
		}
	}

	@Test
	public void variantSetIsNotModifiable() {
		Set<String> variants = new HashSet<String>();
		variants.add("variant1");
		variants.add("variant2");
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, variants);
		Set<String> result = entry.getVariantURIs();
		try {
			result.remove("variant1");
			fail("Should have thrown exception");
		} catch (UnsupportedOperationException expected) {
		}
		try {
			result.add("variant3");
			fail("Should have thrown exception");
		} catch (UnsupportedOperationException expected) {
		}
	}

	@Test
	public void canConvertToString() {
		entry = new HttpCacheEntry(new Date(), new Date(), statusLine,
				new Header[]{}, mockResource, null);
		assertNotNull(entry.toString());
		assertFalse("".equals(entry.toString()));
	}
}
