/*
 * Copyright 2011 Witoslaw Koczewsi <wi@koczewski.de>, Artjom Kochtchi
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package scrum.server.issues;

import ilarkesto.base.Str;
import ilarkesto.core.logging.Log;
import ilarkesto.io.IO;
import ilarkesto.persistence.TransactionService;
import ilarkesto.webapp.Servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import scrum.server.ScrumWebApplication;
import scrum.server.WebSession;
import scrum.server.common.AHttpServlet;
import scrum.server.journal.ProjectEventDao;
import scrum.server.project.Project;
import scrum.server.project.ProjectDao;

public class IssueServlet extends AHttpServlet {

	private static final long serialVersionUID = 1;

	private static Log log = Log.get(IssueServlet.class);

	private transient IssueDao issueDao;
	private transient ProjectDao projectDao;
	private transient ProjectEventDao projectEventDao;
	private transient TransactionService transactionService;

	private ScrumWebApplication app;

	@Override
	protected void onRequest(HttpServletRequest req, HttpServletResponse resp, WebSession session) throws IOException {
		req.setCharacterEncoding(IO.UTF_8);

		String projectId = req.getParameter("projectId");
		String text = req.getParameter("text");
		String name = Str.cutRight(req.getParameter("name"), 33);
		if (Str.isBlank(name)) name = null;
		String email = Str.cutRight(req.getParameter("email"), 66);
		if (Str.isBlank(email)) email = null;
		boolean publish = Str.isTrue(req.getParameter("publish"));

		log.info("Message from the internets");
		log.info("    projectId: " + projectId);
		log.info("    name: " + name);
		log.info("    email: " + email);
		log.info("    publish: " + publish);
		log.info("    text: " + text);
		log.info("  Request-Data:");
		log.info(Servlet.toString(req, "        "));

		String message;
		try {
			message = submitIssue(projectId, text, name, email, publish);
		} catch (Throwable ex) {
			log.error("Submitting issue failed.", "\n" + Servlet.toString(req, "  "), ex);
			message = "<h2>Failure</h2><p>Submitting your feedback failed: <strong>" + Str.getRootCauseMessage(ex)
					+ "</strong></p><p>We are sorry, please try again later.</p>";
		}

		String returnUrl = req.getParameter("returnUrl");
		if (returnUrl == null) returnUrl = "http://kunagi.org/message.html#{message}";
		returnUrl = returnUrl.replace("{message}", Str.encodeUrlParameter(message));

		resp.sendRedirect(returnUrl);
	}

	private String submitIssue(String projectId, String text, String name, String email, boolean publish) {
		if (projectId == null) throw new RuntimeException("projectId == null");
		Project project = projectDao.getById(projectId);
		Issue issue = issueDao.postIssue(project, "Message from the Internets", "<nowiki>" + text + "</nowiki>", name,
			email, publish);
		if (publish) {
			project.updateHomepage(issue, true);
		}
		String issuer = issue.getIssuer();
		if (Str.isBlank(issuer)) issuer = "anonymous";
		projectEventDao.postEvent(project, issuer + " submitted " + issue.getReferenceAndLabel(), issue);
		transactionService.commit();

		app.sendToConversationsByProject(project, issue);

		String issueLink = publish ? "<a href=\"" + issue.getReference() + ".html\">" + issue.getReference() + "</a>"
				: "<code>" + issue.getReference() + "</code>";
		return "<h2>Feedback submitted</h2><p>Thank you for your feedback!</p><p>Your issue is now known as "
				+ issueLink + " and will be reviewed by our Product Owner shortly.</p>";
	}

	@Override
	protected void onInit(ServletConfig config) {
		app = ScrumWebApplication.get(config);
		issueDao = app.getIssueDao();
		projectDao = app.getProjectDao();
		transactionService = app.getTransactionService();
		projectEventDao = app.getProjectEventDao();
	}

}