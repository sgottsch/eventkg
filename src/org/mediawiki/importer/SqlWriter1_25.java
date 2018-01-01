/*
 * MediaWiki import/export processing tools
 * Copyright 2005 by Brion Vibber
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * $Id$
 */

package org.mediawiki.importer;

import java.io.IOException;
import java.nio.charset.Charset;

public class SqlWriter1_25 extends SqlWriter15 {
	public SqlWriter1_25(SqlWriter.Traits tr, SqlStream output) {
		super(tr, output);
	}
	
	public SqlWriter1_25(SqlWriter.Traits tr, SqlStream output, String prefix) {
		super(tr, output, prefix);
	}
	
	protected void updatePage(Page page, Revision revision) throws IOException {
		bufferInsertRow("page", new Object[][] {
				{"page_id", new Integer(page.Id)},
				{"page_namespace", page.Title.Namespace},
				{"page_title", titleFormat(page.Title.Text)},
				{"page_restrictions", page.Restrictions},
				{"page_is_redirect", page.isRedirect ? ONE : ZERO},
				{"page_is_new", ZERO},
				{"page_random", traits.getRandom()},
				{"page_touched", traits.getCurrentTime()},
				{"page_latest", new Integer(revision.Id)},
				{"page_len", revision.Bytes},
				{"page_content_model", revision.Model},
		});

		if (page.Redirect != null) {
			bufferInsertRow("redirect", new Object[][] {
				{"rd_from", new Integer(page.Id)},
				{"rd_namespace", page.Redirect.Namespace},
				{"rd_title", titleFormat(page.Redirect.Text)},
			});
		}
		checkpoint();
	}

	public void writeRevision(Revision revision) throws IOException {
		bufferInsertRow(traits.getTextTable(), new Object[][] {
				{"old_id", new Integer(revision.Id)},
				{"old_text", revision.Text == null ? "" : revision.Text},
				{"old_flags", "utf-8"}});
		
		int rev_deleted = 0;
		if (revision.Contributor.Username==null) rev_deleted |= DELETED_USER;
		if (revision.Comment==null) rev_deleted |= DELETED_COMMENT;
		if (revision.Text==null) rev_deleted |= DELETED_TEXT;

		bufferInsertRow("revision", new Object[][] {
				{"rev_id", new Integer(revision.Id)},
				{"rev_page", new Integer(currentPage.Id)},
				{"rev_text_id", new Integer(revision.Id)},
				{"rev_comment", commentFormat(revision.Comment)},
				{"rev_user", revision.Contributor.Username == null ? ZERO :  new Integer(revision.Contributor.Id)},
				{"rev_user_text", revision.Contributor.Username == null ? "" : revision.Contributor.Username},
				{"rev_timestamp", timestampFormat(revision.Timestamp)},
				{"rev_minor_edit", revision.Minor ? ONE : ZERO},
				{"rev_parent_id", revision.Parentid == 0 ? null : new Integer(revision.Parentid)},
				{"rev_sha1", revision.Sha1},
				{"rev_content_model", revision.Model},
				{"rev_content_format", revision.Format},
				{"rev_deleted", rev_deleted==0 ? ZERO : new Integer(rev_deleted)},
				{"rev_len", revision.Bytes},
		});
		
		lastRevision = revision;
	}
}
