/*
 *  weMessage - iMessage for Android
 *  Copyright (C) 2018 Roman Scott
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package scott.wemessage.app.models.chats;

import java.util.ArrayList;
import java.util.List;

import scott.wemessage.app.models.users.Handle;
import scott.wemessage.app.utils.FileLocationContainer;
import scott.wemessage.commons.utils.StringUtils;

public class GroupChat extends Chat {

    private String displayName;
    private List<Handle> participants;
    private boolean isDoNotDisturb;

    public GroupChat(){

    }

    public GroupChat(String identifier, FileLocationContainer groupChatPictureFileLocation, String macGuid, String macGroupID, String macChatIdentifier,
                     boolean isInChat, boolean hasUnreadMessages, boolean isDoNotDisturb, String displayName, List<Handle> participants) {

        super(identifier, groupChatPictureFileLocation, macGuid, macGroupID, macChatIdentifier, isInChat, hasUnreadMessages);

        this.displayName = displayName;
        this.participants = participants;
        this.isDoNotDisturb = isDoNotDisturb;
    }

    @Override
    public ChatType getChatType() {
        return ChatType.GROUP;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUIDisplayName(){
        String fullString;

        if (!StringUtils.isEmpty(getDisplayName())){
            fullString = getDisplayName();
        } else {
            ArrayList<String> dummyParticipantList = new ArrayList<>();

            for (Handle h : participants) {
                dummyParticipantList.add(h.getDisplayName());
            }
            dummyParticipantList.remove(dummyParticipantList.size() - 1);

            fullString = StringUtils.join(dummyParticipantList, ", ", 2) + " & " + participants.get(participants.size() - 1).getDisplayName();
        }
        return fullString;
    }

    public List<Handle> getParticipants() {
        return participants;
    }

    public boolean isDoNotDisturb() {
        return isDoNotDisturb;
    }

    public GroupChat setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public GroupChat setParticipants(List<Handle> participants) {
        this.participants = participants;
        return this;
    }

    public GroupChat addParticipant(Handle handle){
        participants.add(handle);
        return this;
    }

    public GroupChat removeParticipant(Handle handle){
        for (Handle h : participants){
            if (h.getUuid().toString().equals(handle.getUuid().toString())){
                participants.remove(h);
                return this;
            }
        }
        return this;
    }

    public GroupChat setDoNotDisturb(boolean isDoNotDisturb) {
        this.isDoNotDisturb = isDoNotDisturb;
        return this;
    }
}