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
package scrum.client.calendar;

import ilarkesto.core.scope.Scope;
import scrum.client.common.TooltipBuilder;
import scrum.client.workspace.ProjectWorkspaceWidgets;

public class CreateSimpleEventAction extends GCreateSimpleEventAction {

	@Override
	public String getLabel() {
		return "Create Event";
	}

	@Override
	protected void updateTooltip(TooltipBuilder tb) {
		tb.setText("Create new Event on the date selected.");
		if (!getCurrentProject().isScrumTeamMember(getCurrentUser())) tb.addRemark(TooltipBuilder.NOT_SCRUMTEAM);
	}

	@Override
	public boolean isExecutable() {
		return true;
	}

	@Override
	public boolean isPermitted() {
		if (!getCurrentProject().isScrumTeamMember(getCurrentUser())) return false;
		return true;
	}

	@Override
	protected void onExecute() {
		CalendarWidget calendar = Scope.get().getComponent(ProjectWorkspaceWidgets.class).getCalendar();
		SimpleEvent event = new SimpleEvent(getCurrentProject(), calendar.getSelectedDate());
		getDao().createSimpleEvent(event);
		Scope.get().getComponent(ProjectWorkspaceWidgets.class).showEntity(event);
	}

}