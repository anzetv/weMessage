package scott.wemessage.server.messages;

import org.apache.commons.io.FilenameUtils;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import scott.wemessage.commons.crypto.AESCrypto;
import scott.wemessage.commons.json.message.JSONAttachment;
import scott.wemessage.commons.json.message.JSONChat;
import scott.wemessage.commons.json.message.JSONMessage;
import scott.wemessage.commons.json.message.security.JSONEncryptedFile;
import scott.wemessage.commons.json.message.security.JSONEncryptedText;
import scott.wemessage.commons.types.MimeType;
import scott.wemessage.commons.utils.DateUtils;
import scott.wemessage.commons.utils.FileUtils;
import scott.wemessage.server.ServerLogger;
import scott.wemessage.server.commands.AppleScriptExecutor;
import scott.wemessage.server.configuration.ServerConfiguration;
import scott.wemessage.server.messages.chat.ChatBase;
import scott.wemessage.server.messages.chat.GroupChat;
import scott.wemessage.server.messages.chat.PeerChat;
import scott.wemessage.server.weMessage;

public class Message {

    private String guid;
    private int rowID;
    private ChatBase chat;
    private Handle handle;
    private String text;
    private List<Attachment> attachments;
    private Integer dateSent, dateDelivered, dateRead;
    private Boolean errored, isSent, isDelivered, isRead, isFinished, isFromMe;

    public Message(){
        this(null, -1, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Message(String guid, int rowID, ChatBase chat, Handle handle, List<Attachment> attachments, String text, Integer dateSent, Integer dateDelivered, Integer dateRead,
                   Boolean errored, Boolean isSent, Boolean isDelivered, Boolean isRead, Boolean isFinished, Boolean isFromMe){
        this.guid = guid;
        this.rowID = rowID;
        this.chat = chat;
        this.handle = handle;
        this.text = text;
        this.attachments = attachments;
        this.dateSent = dateSent;
        this.dateDelivered = dateDelivered;
        this.dateRead = dateRead;
        this.errored = errored;
        this.isSent = isSent;
        this.isDelivered = isDelivered;
        this.isRead = isRead;
        this.isFinished = isFinished;
        this.isFromMe = isFromMe;
    }

    public String getGuid() {
        return guid;
    }

    public int getRowID() {
        return rowID;
    }

    public ChatBase getChat() {
        return chat;
    }

    public Handle getHandle() {
        return handle;
    }

    public String getText() {
        return text;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public Integer getDateSent() {
        return dateSent;
    }

    public Integer getDateDelivered() {
        return dateDelivered;
    }

    public Integer getDateRead() {
        return dateRead;
    }

    public Date getModernDateSent(){
        if (dateSent == null || dateSent == -1) return null;

        return DateUtils.getDateUsing2001(dateSent);
    }

    public Date getModernDateDelivered(){
        if (dateDelivered == null || dateDelivered == -1) return null;

        return DateUtils.getDateUsing2001(dateDelivered);
    }

    public Date getModernDateRead(){
        if (dateRead == null || dateRead == -1) return null;

        return DateUtils.getDateUsing2001(dateRead);
    }

    public Boolean hasErrored() {
        return errored;
    }

    public Boolean isSent() {
        return isSent;
    }

    public Boolean isDelivered() {
        return isDelivered;
    }

    public Boolean isRead() {
        return isRead;
    }

    public Boolean isFinished() {
        return isFinished;
    }

    public Boolean isFromMe() {
        return isFromMe;
    }

    public Boolean hasAttachments(){
        return !attachments.isEmpty();
    }

    public Message setGuid(String guid) {
        this.guid = guid;
        return this;
    }

    public Message setRowID(int rowID) {
        this.rowID = rowID;
        return this;
    }

    public Message setChat(ChatBase chat) {
        this.chat = chat;
        return this;
    }

    public Message setHandle(Handle handle) {
        this.handle = handle;
        return this;
    }

    public Message setText(String text) {
        this.text = text;
        return this;
    }

    public Message setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    public Message setDateSent(Integer dateSent) {
        this.dateSent = dateSent;
        return this;
    }

    public Message setDateDelivered(Integer dateDelivered) {
        this.dateDelivered = dateDelivered;
        return this;
    }

    public Message setDateRead(Integer dateRead) {
        this.dateRead = dateRead;
        return this;
    }

    public Message setErrored(boolean errored) {
        this.errored = errored;
        return this;
    }

    public Message setSent(boolean sent) {
        isSent = sent;
        return this;
    }

    public Message setDelivered(boolean delivered) {
        isDelivered = delivered;
        return this;
    }

    public Message setRead(boolean read) {
        isRead = read;
        return this;
    }

    public Message setFinished(boolean finished) {
        isFinished = finished;
        return this;
    }

    public Message setFromMe(boolean fromMe) {
        isFromMe = fromMe;
        return this;
    }

    public JSONMessage toJson(ServerConfiguration serverConfiguration, AppleScriptExecutor appleScriptExecutor) throws IOException, GeneralSecurityException {
        ChatBase chat = getChat();
        List<String> participants = new ArrayList<>();
        List<JSONAttachment> attachments = new ArrayList<>();
        String displayName;
        String handle;

        if(chat instanceof PeerChat){
            displayName = null;
            participants.add(((PeerChat) chat).getPeer().getHandleID());
        } else {
            GroupChat groupChat = (GroupChat) chat;

            displayName = groupChat.getDisplayName();

            for (Handle h : groupChat.getParticipants()){
                participants.add(h.getHandleID());
            }
        }

        if (getHandle() == null){
            handle = null;
        }else {
            handle = getHandle().getHandleID();
        }

        for (Attachment attachment : getAttachments()) {
            String filePath = attachment.getFileLocation().replace("~", System.getProperty("user.home"));

            if (new File(filePath).length() > weMessage.MAX_FILE_SIZE) {
                ServerLogger.log(ServerLogger.Level.WARNING, "Could not send attachment: " + attachment.getTransferName()
                        + " because it exceeds the maximum file size of " + FileUtils.getFileSizeString(weMessage.MAX_FILE_SIZE));
            } else {
                if (MimeType.MimeExtension.getExtensionFromString(attachment.getFileType()) == MimeType.MimeExtension.MOV) {
                    if (!serverConfiguration.getConfigJSON().getConfig().getTranscodeVideos()) continue;

                    FFmpeg ffmpeg = new FFmpeg(serverConfiguration.getConfigJSON().getConfig().getFfmpegLocation());
                    String outputName = FilenameUtils.removeExtension(attachment.getTransferName()) + ".webm";

                    ServerLogger.log(ServerLogger.Level.INFO, "An encoding process has just started for file: " + outputName + ". This allows videos to be played on Android devices.");
                    ServerLogger.log(ServerLogger.Level.INFO, "Please do not turn off the server, or the message will not send.");
                    ServerLogger.emptyLine();

                    FFmpegBuilder ffmpegBuilder = new FFmpegBuilder()
                            .setInput(filePath)
                            .overrideOutputFiles(true)
                            .addOutput(appleScriptExecutor.getTempFolder().toString() + "/" + outputName)
                            .setFormat("webm")
                            .setVideoCodec("libvpx-vp9")
                            .setAudioCodec("libopus")
                            .addExtraArgs("-b:v", "1400K")
                            .addExtraArgs("-crf", "26")
                            .addExtraArgs("-threads", "8")
                            .addExtraArgs("-speed", "4")
                            .addExtraArgs("-tile-columns", "6")
                            .addExtraArgs("-frame-parallel", "1")
                            .addExtraArgs("-auto-alt-ref", "1")
                            .addExtraArgs("-lag-in-frames", "25")
                            .done();

                    long startTime = System.nanoTime();

                    FFmpegExecutor ffmpegExecutor = new FFmpegExecutor(ffmpeg);
                    ffmpegExecutor.createJob(ffmpegBuilder).run();

                    long finishTime = System.nanoTime();

                    ServerLogger.log(ServerLogger.Level.INFO, "Finished encoding process. Time: " + (((finishTime - startTime) / 1000000L) / 1000L) + " seconds.");

                    File encodedFile = new File(appleScriptExecutor.getTempFolder().toString(), outputName);
                    byte[] fileBytes = FileUtils.readBytesFromFile(encodedFile);
                    String keys = AESCrypto.keysToString(AESCrypto.generateKeys());
                    AESCrypto.CipherByteArrayIvMac byteArrayIvMac = AESCrypto.encryptBytes(fileBytes, keys);

                    JSONAttachment jsonAttachment = new JSONAttachment(
                            attachment.getGuid(),
                            outputName,
                            MimeType.MimeExtension.WEBM.getTypeString(),
                            new JSONEncryptedFile(
                                    byteArrayIvMac.getCipherBytes(),
                                    keys,
                                    byteArrayIvMac.joinedIvAndMac()
                            ),
                            Math.round(encodedFile.length())
                    );
                    attachments.add(jsonAttachment);
                } else {
                    byte[] fileBytes = FileUtils.readBytesFromFile(new File(filePath));

                    String keys = AESCrypto.keysToString(AESCrypto.generateKeys());
                    AESCrypto.CipherByteArrayIvMac byteArrayIvMac = AESCrypto.encryptBytes(fileBytes, keys);

                    JSONAttachment jsonAttachment = new JSONAttachment(
                            attachment.getGuid(),
                            attachment.getTransferName(),
                            attachment.getFileType(),
                            new JSONEncryptedFile(
                                    byteArrayIvMac.getCipherBytes(),
                                    keys,
                                    byteArrayIvMac.joinedIvAndMac()
                            ),
                            attachment.getTotalBytes()
                    );
                    attachments.add(jsonAttachment);
                }
            }
        }

        String keys = AESCrypto.keysToString(AESCrypto.generateKeys());
        String encryptedText = AESCrypto.encryptString(getText(), keys);

        return new JSONMessage(
                getGuid(),
                new JSONChat(
                        getChat().getGuid(),
                        getChat().getGroupID(),
                        getChat().getChatIdentifier(),
                        displayName,
                        participants
                ),
                handle,
                attachments,
                new JSONEncryptedText(
                        encryptedText,
                        keys
                ),
                getDateSent(),
                getDateDelivered(),
                getDateRead(),
                hasErrored(),
                isSent(),
                isDelivered(),
                isRead(),
                isFinished(),
                isFromMe()
        );
    }
}