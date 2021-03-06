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

package scott.wemessage.app.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;
import com.thefinestartist.finestwebview.FinestWebView;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

import scott.wemessage.R;
import scott.wemessage.app.AppLogger;
import scott.wemessage.app.connection.ConnectionService;
import scott.wemessage.app.connection.ConnectionServiceConnection;
import scott.wemessage.app.connection.IConnectionBinder;
import scott.wemessage.app.messages.MessageCallbacks;
import scott.wemessage.app.messages.MessageDatabase;
import scott.wemessage.app.messages.MessageManager;
import scott.wemessage.app.messages.notifications.NotificationCallbacks;
import scott.wemessage.app.models.chats.Chat;
import scott.wemessage.app.models.chats.GroupChat;
import scott.wemessage.app.models.chats.PeerChat;
import scott.wemessage.app.models.messages.ActionMessage;
import scott.wemessage.app.models.messages.Attachment;
import scott.wemessage.app.models.messages.Message;
import scott.wemessage.app.models.messages.MessageBase;
import scott.wemessage.app.models.sms.chats.SmsChat;
import scott.wemessage.app.models.sms.chats.SmsGroupChat;
import scott.wemessage.app.models.sms.chats.SmsPeerChat;
import scott.wemessage.app.models.sms.messages.MmsMessage;
import scott.wemessage.app.models.users.ContactInfo;
import scott.wemessage.app.models.users.Handle;
import scott.wemessage.app.sms.MmsManager;
import scott.wemessage.app.ui.activities.ChatListActivity;
import scott.wemessage.app.ui.activities.ChatViewActivity;
import scott.wemessage.app.ui.activities.ContactListActivity;
import scott.wemessage.app.ui.activities.ContactViewActivity;
import scott.wemessage.app.ui.activities.ConversationActivity;
import scott.wemessage.app.ui.activities.LaunchActivity;
import scott.wemessage.app.ui.activities.mini.MessageImageActivity;
import scott.wemessage.app.ui.activities.mini.MessageVideoActivity;
import scott.wemessage.app.ui.view.chat.ChatTitleView;
import scott.wemessage.app.ui.view.dialog.DialogDisplayer;
import scott.wemessage.app.ui.view.messages.ActionMessageView;
import scott.wemessage.app.ui.view.messages.ActionMessageViewHolder;
import scott.wemessage.app.ui.view.messages.IncomingMessageViewHolder;
import scott.wemessage.app.ui.view.messages.MessageEffects;
import scott.wemessage.app.ui.view.messages.MessageView;
import scott.wemessage.app.ui.view.messages.MessageViewHolder;
import scott.wemessage.app.ui.view.messages.OutgoingMessageViewHolder;
import scott.wemessage.app.utils.AndroidUtils;
import scott.wemessage.app.utils.FileLocationContainer;
import scott.wemessage.app.utils.IOUtils;
import scott.wemessage.app.utils.media.AudioAttachmentMediaPlayer;
import scott.wemessage.app.utils.media.MediaDownloadCallbacks;
import scott.wemessage.app.utils.view.DisplayUtils;
import scott.wemessage.app.weMessage;
import scott.wemessage.commons.connection.json.action.JSONAction;
import scott.wemessage.commons.types.FailReason;
import scott.wemessage.commons.types.MessageEffect;
import scott.wemessage.commons.types.MimeType;
import scott.wemessage.commons.types.ReturnType;
import scott.wemessage.commons.utils.DateUtils;
import scott.wemessage.commons.utils.FileUtils;
import scott.wemessage.commons.utils.ListUtils;
import scott.wemessage.commons.utils.StringUtils;

public class ConversationFragment extends MessagingFragment implements MessageCallbacks, NotificationCallbacks, MediaDownloadCallbacks, IConnectionBinder,
        AudioAttachmentMediaPlayer.AttachmentAudioCallbacks, AttachmentPopupFragment.AttachmentInputListener, MessageEffects.AnimationCallbacks {

    private AtomicBoolean isGlobalAnimationPlaying = new AtomicBoolean(false);
    private final Object chatLock = new Object();

    private final String TAG = "ConversationFragment";
    private final String BUNDLE_DOWNLOAD_TASKS = "bundleDownloadTasks";
    private final long MESSAGE_QUEUE_AMOUNT = 25;
    private final int ERROR_SNACKBAR_DURATION = 5;
    private final byte CONTENT_TYPE_ACTION = 22;

    private String callbackUuid;
    private String lastMessageId = null;
    private boolean isBoundToConnectionService = false;
    private boolean isPopupFragmentOpen = false;
    private boolean isSelectionMode = false;

    private String cameraAttachmentInput;
    private String voiceMessageInput;
    private String tempPermissionDownloadAttachment;
    private List<String> attachmentsInput = new ArrayList<>();

    private Chat chat;
    private ChatTitleView chatTitleView;
    private MessageInput messageInput;
    private MessagesList messageList;
    private MessagesListAdapter<IMessage> messageListAdapter;

    private View bottomDivider;
    private BottomSheetLayout conversationBottomSheet;
    private FrameLayout galleryFragmentContainer;
    private RelativeLayout messageSelectionModeBar;
    private LinearLayout conversationLayout;
    private RelativeLayout animationLayout;
    private Toolbar toolbar;

    private AudioAttachmentMediaPlayer audioAttachmentMediaPlayer;
    private ConnectionServiceConnection serviceConnection = new ConnectionServiceConnection();

    private ConcurrentHashMap<String, MessageView> selectedMessages = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Message> messageMapIntegrity = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ActionMessage> actionMessageMapIntegrity = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> downloadTasks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Message> messageAnimations = new ConcurrentHashMap<>();
    private WeakHashMap<String, Long> webViewClicks = new WeakHashMap<>();

    private BroadcastReceiver messageListBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(weMessage.BROADCAST_CONNECTION_SERVICE_STOPPED)){
                unbindService();
            }else if(intent.getAction().equals(weMessage.BROADCAST_DISCONNECT_REASON_SERVER_CLOSED)){
                showDisconnectReasonDialog(intent, getString(R.string.connection_error_server_closed_message), new Runnable() {
                    @Override
                    public void run() {
                        LaunchActivity.launchActivity(getActivity(), ConversationFragment.this, false);
                    }
                });
            }else if(intent.getAction().equals(weMessage.BROADCAST_DISCONNECT_REASON_ERROR)){
                showDisconnectReasonDialog(intent, getString(R.string.connection_error_unknown_message), new Runnable() {
                    @Override
                    public void run() {
                        LaunchActivity.launchActivity(getActivity(), ConversationFragment.this, false);
                    }
                });
            }else if(intent.getAction().equals(weMessage.BROADCAST_DISCONNECT_REASON_FORCED)){
                showDisconnectReasonDialog(intent, getString(R.string.connection_error_force_disconnect_message), new Runnable() {
                    @Override
                    public void run() {
                        LaunchActivity.launchActivity(getActivity(), ConversationFragment.this, false);
                    }
                });
            }else if(intent.getAction().equals(weMessage.BROADCAST_DISCONNECT_REASON_CLIENT_DISCONNECTED)){
                showDisconnectReasonDialog(intent, getString(R.string.connection_error_client_disconnect_message), new Runnable() {
                    @Override
                    public void run() {
                        LaunchActivity.launchActivity(getActivity(), ConversationFragment.this, false);
                    }
                });
            }else if(intent.getAction().equals(weMessage.BROADCAST_NEW_MESSAGE_ERROR)){
                showErroredSnackbar(getString(R.string.new_message_error));
            }else if(intent.getAction().equals(weMessage.BROADCAST_SEND_MESSAGE_ERROR)){
                showErroredSnackbar(getString(R.string.send_message_error));
            }else if(intent.getAction().equals(weMessage.BROADCAST_MESSAGE_UPDATE_ERROR)) {
                showErroredSnackbar(getString(R.string.message_update_error));
            }else if(intent.getAction().equals(weMessage.BROADCAST_ACTION_PERFORM_ERROR)){
                if (intent.getExtras() != null){
                    showErroredSnackbar(intent.getStringExtra(weMessage.BUNDLE_ACTION_PERFORM_ALTERNATE_ERROR_MESSAGE));
                }else {
                    showErroredSnackbar(getString(R.string.action_perform_error_default));
                }
            }else if(intent.getAction().equals(weMessage.BROADCAST_RESULT_PROCESS_ERROR)){
                showErroredSnackbar(getString(R.string.result_process_error));
            }else if(intent.getAction().equals(weMessage.BROADCAST_LOAD_ATTACHMENT_ERROR)){
                showErroredSnackbar(getString(R.string.load_attachment_error));
            }else if(intent.getAction().equals(weMessage.BROADCAST_PLAY_AUDIO_ATTACHMENT_ERROR)){
                showErroredSnackbar(getString(R.string.play_audio_attachment_error));
            }else if (intent.getAction().equals(weMessage.BROADCAST_IMAGE_FULLSCREEN_ACTIVITY_START)){
                launchFullScreenImageActivity(intent.getStringExtra(weMessage.BUNDLE_FULL_SCREEN_IMAGE_URI));
            }else if (intent.getAction().equals(weMessage.BROADCAST_VIDEO_FULLSCREEN_ACTIVITY_START)){
                launchFullScreenVideoActivity(intent.getStringExtra(weMessage.BUNDLE_FULL_SCREEN_VIDEO_URI));
            }else if(intent.getAction().equals(weMessage.BROADCAST_CONTACT_SYNC_FAILED)){
                DialogDisplayer.showContactSyncResult(false, getActivity(), getFragmentManager());
            }else if(intent.getAction().equals(weMessage.BROADCAST_CONTACT_SYNC_SUCCESS)){
                DialogDisplayer.showContactSyncResult(true, getActivity(), getFragmentManager());
            }else if(intent.getAction().equals(weMessage.BROADCAST_NO_ACCOUNTS_FOUND_NOTIFICATION)){
                DialogDisplayer.showNoAccountsFoundDialog(getActivity(), getFragmentManager());
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (isConnectionServiceRunning()){
            bindService();
        }

        MessageManager messageManager = weMessage.get().getMessageManager();
        MessageDatabase messageDatabase = weMessage.get().getMessageDatabase();

        if (savedInstanceState == null) {
            try {
                Intent startingIntent = getActivity().getIntent();
                Class startingClass = Class.forName(startingIntent.getStringExtra(weMessage.BUNDLE_RETURN_POINT));

                if (startingIntent.getStringExtra(weMessage.BUNDLE_CONVERSATION_CHAT).equals(weMessage.BUNDLE_GO_TO_CONTACT_LIST)){
                    Intent launchIntent = new Intent(weMessage.get(), ContactListActivity.class);

                    startActivity(launchIntent);
                    getActivity().finish();
                } else {
                    Chat chat = messageDatabase.getChatByIdentifier(startingIntent.getStringExtra(weMessage.BUNDLE_CONVERSATION_CHAT));

                    if (chat == null) {
                        Intent returnIntent = new Intent(weMessage.get(), startingClass);
                        returnIntent.putExtra(weMessage.BUNDLE_CONVERSATION_GO_BACK_REASON, getString(R.string.conversation_load_failure));

                        startActivity(returnIntent);
                        getActivity().finish();
                    } else {
                        setChat(chat);
                    }
                }
            } catch (Exception ex) {
                AppLogger.error(TAG, "Could not load ConversationFragment because an error occurred", ex);
                goToChatList(getString(R.string.conversation_load_failure));
            }
        }else {
            setChat(messageDatabase.getChatByIdentifier(savedInstanceState.getString(weMessage.BUNDLE_CONVERSATION_CHAT)));
            attachmentsInput = savedInstanceState.getStringArrayList(weMessage.BUNDLE_SELECTED_GALLERY_STORE);
            cameraAttachmentInput = savedInstanceState.getString(weMessage.BUNDLE_CAMERA_ATTACHMENT_FILE);
            voiceMessageInput = savedInstanceState.getString(weMessage.BUNDLE_VOICE_MESSAGE_INPUT_FILE);

            try {
                downloadTasks = (ConcurrentHashMap<String, String>) savedInstanceState.getSerializable(BUNDLE_DOWNLOAD_TASKS);
            }catch (Exception ex){
                Serializable downloadTasksMap = savedInstanceState.getSerializable(BUNDLE_DOWNLOAD_TASKS);

                if (downloadTasksMap instanceof Map){
                    downloadTasks.putAll((Map) downloadTasksMap);
                }
            }
        }

        if (getChat() == null){
            super.onCreate(savedInstanceState);
            return;
        }

        IntentFilter broadcastIntentFilter = new IntentFilter();

        broadcastIntentFilter.addAction(weMessage.BROADCAST_CONNECTION_SERVICE_STOPPED);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_DISCONNECT_REASON_SERVER_CLOSED);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_DISCONNECT_REASON_ERROR);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_DISCONNECT_REASON_FORCED);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_DISCONNECT_REASON_CLIENT_DISCONNECTED);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_NEW_MESSAGE_ERROR);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_SEND_MESSAGE_ERROR);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_MESSAGE_UPDATE_ERROR);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_ACTION_PERFORM_ERROR);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_RESULT_PROCESS_ERROR);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_LOAD_ATTACHMENT_ERROR);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_PLAY_AUDIO_ATTACHMENT_ERROR);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_IMAGE_FULLSCREEN_ACTIVITY_START);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_VIDEO_FULLSCREEN_ACTIVITY_START);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_CONTACT_SYNC_FAILED);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_CONTACT_SYNC_SUCCESS);
        broadcastIntentFilter.addAction(weMessage.BROADCAST_NO_ACCOUNTS_FOUND_NOTIFICATION);

        callbackUuid = UUID.randomUUID().toString();

        messageManager.hookCallbacks(callbackUuid, this);
        weMessage.get().getNotificationManager().setNotificationCallbacks(this);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageListBroadcastReceiver, broadcastIntentFilter);

        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_conversation, container, false);

        if (getChat() == null) return view;

        Toolbar toolbar = getActivity().findViewById(R.id.conversationToolbar);
        ImageButton backButton = toolbar.findViewById(R.id.conversationBackButton);
        ImageButton infoButton = toolbar.findViewById(R.id.conversationInfoButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToChatList(null);
            }
        });

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getChat() instanceof PeerChat){
                    launchContactView();
                }else if (getChat() instanceof GroupChat){
                    launchChatView();
                }
            }
        });

        toolbar.setTitle(null);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        this.toolbar = toolbar;

        chatTitleView = toolbar.findViewById(R.id.chatTitleView);
        messageList = view.findViewById(R.id.messagesList);
        messageInput = view.findViewById(R.id.messageInputView);

        bottomDivider = view.findViewById(R.id.messageInputDivider);
        conversationBottomSheet = view.findViewById(R.id.conversationBottomSheetLayout);
        galleryFragmentContainer = view.findViewById(R.id.galleryFragmentContainer);
        messageSelectionModeBar = view.findViewById(R.id.messageSelectionModeBar);
        conversationLayout = getActivity().findViewById(R.id.conversationLayout);
        animationLayout = getActivity().findViewById(R.id.animationLayout);

        String meUuid;
        ImageLoader imageLoader;
        final MessageManager messageManager = weMessage.get().getMessageManager();

        if (getChat() instanceof SmsChat){
            meUuid = weMessage.get().getCurrentSession().getSmsHandle().getUuid().toString();
        }else {
            meUuid = weMessage.get().getCurrentSession().getAccount().getHandle().getUuid().toString();
        }

        MessageHolders messageHolders = new MessageHolders()
                .setIncomingTextConfig(IncomingMessageViewHolder.class, R.layout.incoming_message)
                .setOutcomingTextConfig(OutgoingMessageViewHolder.class, R.layout.outgoing_message)
                .registerContentType(CONTENT_TYPE_ACTION, ActionMessageViewHolder.class, R.layout.message_action, R.layout.message_action, new MessageHolders.ContentChecker() {
                    @Override
                    public boolean hasContentFor(IMessage message, byte type) {
                        switch (type){
                            case CONTENT_TYPE_ACTION:
                                return (message instanceof ActionMessageView);
                        }
                        return false;
                    }
                });

        if (getChat() instanceof PeerChat){
            imageLoader = null;
        } else {
            imageLoader = new ImageLoader() {
                @Override
                public void loadImage(ImageView imageView, String url) {
                    Glide.with(ConversationFragment.this).load(url).into(imageView);
                }
            };
        }

        final MessagesListAdapter<IMessage> messageListAdapter = new MessagesListAdapter<>(meUuid, messageHolders, imageLoader, getChat() instanceof SmsChat);

        messageListAdapter.setDateHeadersFormatter(new DateFormatter.Formatter() {
            @Override
            public String format(Date date) {
                return AndroidUtils.processDate(getContext(), date, false, false);
            }
        });

        messageListAdapter.setLoadMoreListener(new MessagesListAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                weMessage.get().getMessageManager().queueMessages(getChat(), (long) totalItemsCount, MESSAGE_QUEUE_AMOUNT, true);
            }
        });

        messageListAdapter.setOnMessageViewClickListener(new MessagesListAdapter.OnMessageViewClickListener<IMessage>() {
            @Override
            public void onMessageViewClick(View view, IMessage message) {
                if (message instanceof MessageView){
                    if (isSelectionMode){
                        for (int childCount = messageList.getChildCount(), i = 0; i < childCount; ++i) {
                            RecyclerView.ViewHolder holder = messageList.getChildViewHolder(messageList.getChildAt(i));

                            if (holder instanceof MessageViewHolder){
                                MessageViewHolder messageHolder = (MessageViewHolder) holder;

                                if (((MessageViewHolder) holder).getMessageId().equals(message.getId())) {
                                    if (!selectedMessages.containsKey(message.getId())) {
                                        selectedMessages.put(message.getId(), (MessageView) message);
                                        messageHolder.setSelected(true);
                                    } else {
                                        selectedMessages.remove(message.getId());
                                        messageHolder.setSelected(false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        messageListAdapter.setOnMessageViewLongClickListener(new MessagesListAdapter.OnMessageViewLongClickListener<IMessage>() {
            @Override
            public void onMessageViewLongClick(View view, final IMessage message) {
                if (message instanceof MessageView) {
                    if (!isSelectionMode) {
                        showMessageOptionsSheetView((MessageView) message);
                    }
                }
            }
        });

        chatTitleView.setChat(getChat());
        messageList.setAdapter(messageListAdapter);
        this.messageListAdapter = messageListAdapter;

        messageInput.setAttachmentsListener(new MessageInput.AttachmentsListener() {
            @Override
            public void onAddAttachments() {
                clearEditText((EditText) messageInput.findViewById(R.id.messageInput), true);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isPopupFragmentOpen) {
                            launchAttachmentPopupFragment(true);
                        }else {
                            closeAttachmentPopupFragment(true);
                        }
                    }
                }, 200);
            }
        });

        messageInput.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(CharSequence input) {
                return sendMessage(input);
            }
        });

        messageInput.findViewById(R.id.messageInput).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isPopupFragmentOpen){
                    closeAttachmentPopupFragment(true);
                }
                return false;
            }
        });

        messageInput.setSaveEnabled(true);

        if (getChat() instanceof SmsChat){
            messageInput.setAttachmentBackgroundDrawable(getSelector(getResources().getColor(R.color.outgoingBubbleColorOrange), getResources().getColor(R.color.outgoingBubbleColorOrangePressed),
                    getResources().getColor(R.color.transparent), R.drawable.mask));

            messageInput.setSendBackgroundDrawable(getSelector(getResources().getColor(R.color.outgoingBubbleColorOrange), getResources().getColor(R.color.outgoingBubbleColorOrangePressed),
                    getResources().getColor(R.color.white_four), R.drawable.mask));
        }

        messageSelectionModeBar.findViewById(R.id.messageSelectCopyIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedMessages.size() == 0){
                    showErroredSnackbar(getString(R.string.selection_mode_no_messages));
                    return;
                }

                copyMessages(new ArrayList<>(selectedMessages.values()));
                toggleSelectionMode(false);
            }
        });

        messageSelectionModeBar.findViewById(R.id.messageSelectDeleteIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedMessages.size() == 0){
                    showErroredSnackbar(getString(R.string.selection_mode_no_messages));
                    return;
                }

                for (MessageView messageView : selectedMessages.values()){
                    weMessage.get().getMessageManager().removeMessage(messageView.getMessage(), true);
                }
                toggleSelectionMode(false);
            }
        });

        messageSelectionModeBar.findViewById(R.id.messageSelectCancelIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSelectionMode(false);
            }
        });

        messageManager.queueMessages(getChat(), 0L, MESSAGE_QUEUE_AMOUNT, true);
        messageManager.setHasUnreadMessages(getChat(), false, true);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(weMessage.BUNDLE_GALLERY_FRAGMENT_OPEN)) {
                launchAttachmentPopupFragment(false);
            }
        }

        toggleIsInChat(chat.isInChat());
        weMessage.get().getNotificationManager().clearNotifications(chat.getIdentifier());

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == weMessage.REQUEST_CODE_CAMERA){
            if (getAttachmentPopupFragment() == null){
                launchAttachmentPopupFragmentWithCameraIntent(resultCode, data);
            }else {
                getAttachmentPopupFragment().onCameraResult(resultCode, data);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(weMessage.BUNDLE_GALLERY_FRAGMENT_OPEN, isPopupFragmentOpen);
        outState.putString(weMessage.BUNDLE_CONVERSATION_CHAT, getChat().getIdentifier());
        outState.putSerializable(BUNDLE_DOWNLOAD_TASKS, downloadTasks);

        if (getAttachmentPopupFragment() != null) {
            outState.putString(weMessage.BUNDLE_CAMERA_ATTACHMENT_FILE, getAttachmentPopupFragment().getCameraAttachmentFile());
            outState.putString(weMessage.BUNDLE_VOICE_MESSAGE_INPUT_FILE, getAttachmentPopupFragment().getAudioFile());
            outState.putStringArrayList(weMessage.BUNDLE_SELECTED_GALLERY_STORE, new ArrayList<>(getAttachmentPopupFragment().getSelectedAttachments()));
        }else {
            outState.putString(weMessage.BUNDLE_CAMERA_ATTACHMENT_FILE, cameraAttachmentInput);
            outState.putString(weMessage.BUNDLE_VOICE_MESSAGE_INPUT_FILE, voiceMessageInput);
            outState.putStringArrayList(weMessage.BUNDLE_SELECTED_GALLERY_STORE, new ArrayList<>(attachmentsInput));
        }

        if (isPopupFragmentOpen){
            closeAttachmentPopupFragment(false);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        pauseAudio(getAudioAttachmentMediaPlayer().getAttachment());

        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (getChat() == null){
            super.onDestroy();
            return;
        }

        MessageManager messageManager = weMessage.get().getMessageManager();

        messageMapIntegrity.clear();
        actionMessageMapIntegrity.clear();

        if (messageListAdapter != null) {
            messageListAdapter.clear();
        }

        messageManager.unhookCallbacks(callbackUuid);
        weMessage.get().getNotificationManager().setNotificationCallbacks(null);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageListBroadcastReceiver);

        if (isBoundToConnectionService){
            unbindService();
        }

        stopAudio();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case weMessage.REQUEST_PERMISSION_WRITE_STORAGE:
                if (isGranted(grantResults)){
                    if (!StringUtils.isEmpty(tempPermissionDownloadAttachment)) {
                        saveMediaToGallery(weMessage.get().getMessageDatabase().getAttachmentByUuid(tempPermissionDownloadAttachment));
                    }
                }
                break;
        }
    }

    @Override
    public void onContactCreate(ContactInfo contact) { }

    @Override
    public void onContactUpdate(ContactInfo oldData, ContactInfo newData) { }

    @Override
    public void onContactListRefresh(List<? extends ContactInfo> handles) { }

    @Override
    public void onChatAdd(Chat chat) { }

    @Override
    public void onChatUpdate(Chat oldData, final Chat newData) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(newData)){
                    setChat(newData);
                    toggleIsInChat(chat.isInChat());
                }
            }
        });
    }

    @Override
    public void onUnreadMessagesUpdate(final Chat chat, boolean hasUnreadMessages) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(chat)){
                    setChat(chat);
                }
            }
        });
    }

    @Override
    public void onChatRename(final Chat chat, String displayName) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(chat)){
                    setChat(chat);
                    chatTitleView.setChat(chat);
                }
            }
        });
    }

    @Override
    public void onParticipantAdd(final Chat chat, Handle handle) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isChatThis(chat)){
                    setChat(chat);
                    chatTitleView.setChat(chat);
                }
            }
        });
    }

    @Override
    public void onParticipantRemove(final Chat chat, Handle handle) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(chat)){
                    setChat(chat);
                    chatTitleView.setChat(chat);
                }
            }
        });
    }

    @Override
    public void onLeaveGroup(final Chat chat) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(chat)){
                    setChat(chat);
                    toggleIsInChat(chat.isInChat());
                }
            }
        });
    }

    @Override
    public void onChatDelete(final Chat chat) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(chat)){
                    goToChatList(getString(R.string.chat_delete_message_go_back));
                }
            }
        });
    }

    @Override
    public void onChatListRefresh(final List<Chat> chats) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!chats.contains(getChat())){
                    goToChatList(null);
                }
            }
        });
    }

    @Override
    public void onMessageAdd(final Message message) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(message.getChat())){
                    if (isMessageBlocked(message)) return;

                    MessageView messageView = new MessageView(message);
                    messageListAdapter.addToStart(messageView, true);
                    messageMapIntegrity.put(message.getIdentifier(), message);
                    weMessage.get().getMessageManager().setHasUnreadMessages(getChat(), false, true);
                    showDeliveryStatusOnLastMessage();
                }
            }
        });
    }

    @Override
    public void onMessageUpdate(final Message oldData, final Message newData) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(newData.getChat())){
                    if (isMessageBlocked(newData)) return;

                    messageListAdapter.update(oldData.getIdentifier(), new MessageView(newData));
                    messageMapIntegrity.put(newData.getIdentifier(), newData);
                    showDeliveryStatusOnLastMessage();
                }else {
                    Chat newChat = newData.getChat();

                    if (isChatThis(oldData.getChat()) && newChat instanceof SmsChat){
                        if (newChat instanceof SmsPeerChat && getChat() instanceof PeerChat) {
                            if (((SmsPeerChat) newChat).getHandle().equals(((PeerChat) getChat()).getHandle())) {
                                launchConversationView(newChat.getIdentifier());
                            }
                        } else if (newChat instanceof SmsGroupChat && getChat() instanceof GroupChat) {
                            boolean isChatThis = ListUtils.areListsEqual(((SmsGroupChat) newChat).getParticipants(), ((GroupChat) getChat()).getParticipants(), new Comparator<Handle>() {
                                @Override
                                public int compare(Handle o1, Handle o2) {
                                    return o1.getHandleID().compareTo(o2.getHandleID());
                                }
                            });

                            if (isChatThis) {
                                launchConversationView(newChat.getIdentifier());
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onMessageDelete(final Message message) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isChatThis(message.getChat())){
                    if (isMessageBlocked(message)) return;

                    messageListAdapter.deleteById(message.getIdentifier());
                    messageMapIntegrity.remove(message.getIdentifier());
                    showDeliveryStatusOnLastMessage();
                }
            }
        });
    }

    @Override
    public void onMessagesQueueFinish(final List<MessageBase> messages) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (messages.size() > 0) {
                    List<IMessage> messageViews = new ArrayList<>();

                    for (MessageBase messageBase : messages){
                        if (messageBase instanceof ActionMessage){
                            ActionMessage actionMessage = (ActionMessage) messageBase;

                            if (isChatThis(actionMessage.getChat())) {
                                if (!actionMessageMapIntegrity.containsKey(actionMessage.getUuid().toString())) {
                                    ActionMessageView messageView = new ActionMessageView(actionMessage);

                                    messageViews.add(messageView);
                                    actionMessageMapIntegrity.put(actionMessage.getUuid().toString(), actionMessage);
                                }
                            }
                        } else if (messageBase instanceof Message){
                            Message message = (Message) messageBase;

                            if (isChatThis(message.getChat())) {
                                if (isMessageBlocked(message)) continue;

                                if (!messageMapIntegrity.containsKey(message.getIdentifier())) {
                                    MessageView messageView = new MessageView(message);

                                    messageViews.add(messageView);
                                    messageMapIntegrity.put(message.getIdentifier(), message);
                                }
                            }
                        }
                    }

                    if (messageViews.size() > 0) {
                        messageListAdapter.addToEnd(messageViews, false);
                    }
                    showDeliveryStatusOnLastMessage();
                }
            }
        });
    }

    @Override
    public void onActionMessageAdd(final ActionMessage message) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isChatThis(message.getChat())){
                    ActionMessageView messageView = new ActionMessageView(message);
                    messageListAdapter.addToStart(messageView, true);
                    actionMessageMapIntegrity.put(message.getUuid().toString(), message);
                }
            }
        });
    }

    @Override
    public void onMessageSendFailure(final ReturnType returnType) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showMessageSendFailureSnackbar(returnType);
            }
        });
    }

    @Override
    public void onActionPerformFailure(final JSONAction jsonAction, final ReturnType returnType) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showActionFailureSnackbar(jsonAction, returnType);
            }
        });
    }

    @Override
    public void onAttachmentSendFailure(final FailReason failReason) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showAttachmentSendFailureSnackbar(failReason);
            }
        });
    }

    @Override
    public void onAttachmentReceiveFailure(final FailReason failReason) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showAttachmentReceiveFailureSnackbar(failReason);
            }
        });
    }

    @Override
    public void onPlaybackStart(Attachment a) {
        for (int childCount = messageList.getChildCount(), i = 0; i < childCount; ++i) {
            RecyclerView.ViewHolder holder = messageList.getChildViewHolder(messageList.getChildAt(i));

            if (holder instanceof MessageViewHolder){
                MessageViewHolder messageHolder = (MessageViewHolder) holder;

                messageHolder.notifyAudioPlaybackStart(a);
            }
        }
    }

    @Override
    public void onPlaybackStop(String attachmentUuid) {
        for (int childCount = messageList.getChildCount(), i = 0; i < childCount; ++i) {
            RecyclerView.ViewHolder holder = messageList.getChildViewHolder(messageList.getChildAt(i));

            if (holder instanceof MessageViewHolder){
                MessageViewHolder messageHolder = (MessageViewHolder) holder;

                messageHolder.notifyAudioPlaybackStop(attachmentUuid);
            }
            audioAttachmentMediaPlayer = null;
        }
    }

    @Override
    public void onAttachmentChange(boolean isEmpty) {
        messageInput.setHasAttachments(!isEmpty);
    }

    @Override
    public void setAttachmentsInput(List<String> attachments, String cameraAttachmentFile, String audioFile) {
        if (isResumed()){

            int attachmentsAdded = 0;
            int attachmentsRemoved = 0;

            for (String s : attachmentsInput){
                if (!attachments.contains(s)){
                    attachmentsRemoved++;
                }
            }

            for (String s : attachments){
                if (!attachmentsInput.contains(s)){
                    attachmentsAdded++;
                }
            }

            if (attachmentsAdded > 0 && attachmentsRemoved > 0){
                if (attachmentsAdded == 1 || attachmentsRemoved == 1){
                    Toast.makeText(getActivity(), getString(R.string.generic_attachments_added_and_removed_toast), Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getActivity(), getString(R.string.attachments_added_and_removed_toast, attachmentsAdded, attachmentsRemoved), Toast.LENGTH_SHORT).show();
                }
            }else if (attachmentsAdded > 0 && attachmentsRemoved == 0){
                if (attachmentsAdded == 1) {
                    Toast.makeText(getActivity(), getString(R.string.single_attachment_added_toast), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.attachments_added_toast, attachmentsAdded), Toast.LENGTH_SHORT).show();
                }
            }else if (attachmentsRemoved > 0 && attachmentsAdded == 0){
                if (attachmentsRemoved == 1) {
                    Toast.makeText(getActivity(), getString(R.string.single_attachment_removed_toast), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.attachments_removed_toast, attachmentsRemoved), Toast.LENGTH_SHORT).show();
                }
            }

            attachmentsInput = new ArrayList<>(attachments);

            if (audioFile != null && !audioFile.equals(voiceMessageInput)){
                Toast.makeText(getActivity(), getString(R.string.audio_recording_added), Toast.LENGTH_SHORT).show();
            }

            if (cameraAttachmentFile != null && !cameraAttachmentFile.equals(cameraAttachmentInput)){
                Toast.makeText(getActivity(), getString(R.string.camera_attachment_added), Toast.LENGTH_SHORT).show();
            }
            cameraAttachmentInput = cameraAttachmentFile;
            voiceMessageInput = audioFile;
        }
    }

    @Override
    public void onMediaDownloadTaskStart(String attachmentUri) {
        downloadTasks.put(attachmentUri, UUID.randomUUID().toString());
    }

    @Override
    public void onMediaDownloadTaskFinish(String attachmentUri) {
        downloadTasks.remove(attachmentUri);
    }

    @Override
    public boolean canMediaDownloadTaskStart(String attachmentUri) {
        return !downloadTasks.containsKey(attachmentUri);
    }

    @Override
    public boolean onNotification(String chatId) {
        if (getChat() instanceof SmsChat){
            return !getChat().getIdentifier().equals(chatId);
        }else {
            return !getChat().getMacGuid().equals(chatId);
        }
    }

    @Override
    public void bindService(){
        Intent intent = new Intent(getActivity(), ConnectionService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_IMPORTANT);
        isBoundToConnectionService = true;
    }

    @Override
    public void unbindService(){
        if (isBoundToConnectionService) {
            getActivity().unbindService(serviceConnection);
            isBoundToConnectionService = false;
        }
    }

    @Override
    public ConcurrentHashMap<String, Message> getAnimatedMessages(){
        return messageAnimations;
    }

    @Override
    public boolean isGlobalEffectPlaying() {
        return isGlobalAnimationPlaying.get();
    }

    @Override
    public void setGlobalEffectPlaying(boolean value) {
        isGlobalAnimationPlaying.set(value);
    }

    public synchronized AudioAttachmentMediaPlayer getAudioAttachmentMediaPlayer(){
        if (audioAttachmentMediaPlayer == null){
            audioAttachmentMediaPlayer = new AudioAttachmentMediaPlayer();
            audioAttachmentMediaPlayer.setCallback(this);
        }
        return audioAttachmentMediaPlayer;
    }

    public ConcurrentHashMap<String, MessageView> getSelectedMessages(){
        return selectedMessages;
    }

    public synchronized ViewGroup getConversationLayout(){
        return conversationLayout;
    }

    public synchronized RelativeLayout getAnimationLayout(){
        return animationLayout;
    }

    public synchronized Toolbar getToolbar(){
        return toolbar;
    }

    public synchronized boolean playAudio(Attachment a){
        try {
            if (getAudioAttachmentMediaPlayer().hasAudio()) {
                getAudioAttachmentMediaPlayer().stopAudioPlayback();
            }
            getAudioAttachmentMediaPlayer().setAttachment(a);
            getAudioAttachmentMediaPlayer().startAudioPlayback(IOUtils.getUriFromFile(a.getFileLocation().getFile()));
            return true;
        }catch(Exception ex){
            AppLogger.error("An error occurred while trying to play Audio Attachment: " + a.getUuid().toString(), ex);
            showErroredSnackbar(getString(R.string.play_audio_attachment_error));
            getAudioAttachmentMediaPlayer().forceRelease();
            return false;
        }
    }

    public synchronized boolean resumeAudio(Attachment a){
        if (getAudioAttachmentMediaPlayer().hasAudio() && getAudioAttachmentMediaPlayer().getAttachment().getUuid().toString().equals(a.getUuid().toString())){
            getAudioAttachmentMediaPlayer().resumeAudioPlayback();
            return true;
        }
        return false;
    }

    public synchronized boolean pauseAudio(Attachment a){
        if (getAudioAttachmentMediaPlayer().hasAudio() && getAudioAttachmentMediaPlayer().getAttachment().getUuid().toString().equals(a.getUuid().toString())){
            getAudioAttachmentMediaPlayer().pauseAudioPlayback();
            return true;
        }
        return false;
    }

    public synchronized boolean stopAudio(){
        if (getAudioAttachmentMediaPlayer().hasAudio()){
            getAudioAttachmentMediaPlayer().stopAudioPlayback();
            return true;
        }
        return false;
    }

    public synchronized void showAttachmentOptionsSheet(final String attachmentUuid){
        if (!isSelectionMode) {
            conversationBottomSheet.showWithSheetView(LayoutInflater.from(getActivity()).inflate(R.layout.sheet_conversation_attachment_options, conversationBottomSheet, false));

            conversationBottomSheet.findViewById(R.id.conversationAttachmentSheetDownloadButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Attachment a = weMessage.get().getMessageDatabase().getAttachmentByUuid(attachmentUuid);

                    saveMediaToGallery(a);
                    conversationBottomSheet.dismissSheet();
                }
            });
        }
    }

    public void launchWebView(String url){
        if (getActivity() != null) {
            Long previousClickTimestamp = webViewClicks.get(callbackUuid);
            long currentTimestamp = SystemClock.uptimeMillis();

            webViewClicks.put(callbackUuid, currentTimestamp);

            if(previousClickTimestamp == null || (currentTimestamp - previousClickTimestamp.longValue() > 1000L)) {
                
                new FinestWebView.Builder(getActivity())
                        .statusBarColorRes(R.color.webViewBlueDark)
                        .toolbarColorRes(R.color.webViewBlue)
                        .titleColorRes(R.color.finestWhite)
                        .urlColorRes(R.color.webViewBlueLight)
                        .iconDefaultColorRes(R.color.finestWhite)
                        .progressBarColorRes(R.color.finestWhite)
                        .stringResCopiedToClipboard(R.string.webview_copy_message)
                        .stringResShareVia(R.string.webview_share_via)
                        .stringResOpenWith(R.string.webview_open_with)
                        .swipeRefreshColorRes(R.color.webViewBlueDark)
                        .titleSize(DisplayUtils.convertSpToPixel(16, getContext()))
                        .menuTextSize(DisplayUtils.convertSpToPixel(14, getContext()))
                        .urlSize(DisplayUtils.convertSpToPixel(12, getContext()))
                        .menuSelector(R.drawable.selector_light_theme)
                        .menuTextGravity(Gravity.CENTER)
                        .menuTextPaddingRightRes(R.dimen.defaultMenuTextPaddingLeft)
                        .dividerHeight(0)
                        .gradientDivider(false)
                        .setCustomAnimations(R.anim.slide_up, R.anim.hold, R.anim.hold, R.anim.slide_down)
                        .show(parseURL(url));
            }
        }
    }

    public synchronized boolean isInSelectionMode(){
        return isSelectionMode;
    }

    public String getLastMessageId(){
        return lastMessageId;
    }

    private AttachmentPopupFragment getAttachmentPopupFragment(){
        if (getChildFragmentManager().findFragmentById(R.id.galleryFragmentContainer) == null) return null;

        return (AttachmentPopupFragment) getChildFragmentManager().findFragmentById(R.id.galleryFragmentContainer);
    }

    private Chat getChat(){
        synchronized (chatLock){
            return chat;
        }
    }

    private void setChat(Chat chat){
        synchronized (chatLock){
            this.chat = chat;
        }
    }

    private boolean sendMessage(CharSequence input){
        UnprocessedMessage unprocessedMessage = preprocessMessage(input);
        if (StringUtils.isEmpty(input.toString().trim()) && unprocessedMessage.hasNoAttachments()) return false;

        if (getChat() instanceof SmsChat){
            if (MmsManager.isDefaultSmsApp()){
                sendMessageAsync(unprocessedMessage, true);
                return true;
            }else {
                DialogDisplayer.generateAlertDialog(getString(R.string.sms_error), getString(R.string.send_message_sms_not_default)).show(getFragmentManager(), "SendSmsErrorAlert");
                return false;
            }
        }else {
            if (isConnectionServiceRunning() && !isStillConnecting()){
                sendMessageAsync(unprocessedMessage, false);
                return true;
            } else {
                if (getChat() instanceof PeerChat){
                    if (MmsManager.isDefaultSmsApp()){
                        sendMessageAsync(unprocessedMessage, true);
                        return true;
                    }else {
                        showOfflineModes();
                        return false;
                    }
                }else {
                    if (MmsManager.isDefaultSmsApp() && isPossibleSmsGroupChat(getChat())){
                        sendMessageAsync(unprocessedMessage, true);
                        return true;
                    }else {
                        showOfflineModes();
                        return false;
                    }
                }
            }
        }
    }

    private void sendMessageAsync(UnprocessedMessage unprocessedMessage, final boolean useMms){
        new AsyncTask<UnprocessedMessage, Void, MessageTaskReturnType>(){
            @Override
            protected MessageTaskReturnType doInBackground(UnprocessedMessage... params) {
                PackedMessage packedMessage = packMessage(params[0], useMms);

                if (packedMessage.getMessage() == null){
                    return packedMessage.getMessageTaskReturnType();
                }

                if (useMms){
                    weMessage.get().getMmsManager().sendMessage((MmsMessage) packedMessage.getMessage());
                }else {
                    serviceConnection.getConnectionService().getConnectionHandler().sendOutgoingMessage(packedMessage.getMessage(), true);
                }

                return MessageTaskReturnType.TASK_PERFORMED;
            }

            @Override
            protected void onPostExecute(MessageTaskReturnType returnType) {
                if (getActivity() != null && isAdded() && !getActivity().isDestroyed() && !getActivity().isFinishing()){
                    if (returnType == MessageTaskReturnType.FILE_SIZE_TOO_LARGE){
                        DialogDisplayer.generateAlertDialog(getString(R.string.max_file_size_alert_title), getString(R.string.max_file_size_alert_message, FileUtils.getFileSizeString(weMessage.MAX_FILE_SIZE)))
                                .show(getFragmentManager(), "AttachmentMaxFileSizeAlert");
                    }else if (returnType == MessageTaskReturnType.NOT_ENOUGH_MEMORY){
                        DialogDisplayer.generateAlertDialog(getString(R.string.max_file_size_alert_title), getString(R.string.memory_file_size_alert_message))
                                .show(getFragmentManager(), "AttachmentMaxMemorySizeAlert");
                    }else if (returnType == MessageTaskReturnType.FILE_SIZE_TOO_LARGE_MMS){
                        DialogDisplayer.generateAlertDialog(getString(R.string.max_file_size_alert_title), getString(R.string.max_file_size_mms_alert_message, FileUtils.getFileSizeString(weMessage.MAX_MMS_ATTACHMENT_SIZE)))
                                .show(getFragmentManager(), "AttachmentMaxFileSizeAlert");
                    }else if (returnType == MessageTaskReturnType.UNSUPPORTED_ATTACHMENTS){
                        DialogDisplayer.generateAlertDialog(getString(R.string.unsupported_attachments), getString(R.string.send_message_sms_unsupported_attachments))
                                .show(getFragmentManager(), "AttachmentMaxMemorySizeAlert");
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, unprocessedMessage);
    }

    private PackedMessage packMessage(UnprocessedMessage unprocessedMessage, boolean useMms){
        List<Long> sizes = new ArrayList<>();
        long totalSize = 0;

        for (String s : unprocessedMessage.getInputAttachments()) {
            File attachmentFile = new File(s);

            if (AndroidUtils.getMimeTypeFromPath(attachmentFile.getAbsolutePath()) != MimeType.IMAGE) return new PackedMessage(null, MessageTaskReturnType.UNSUPPORTED_ATTACHMENTS);
        }

        if (unprocessedMessage.getCameraInput() != null){
            File inputFile = new File(unprocessedMessage.getCameraInput());

            if (inputFile.exists()){
                if (AndroidUtils.getMimeTypeFromPath(inputFile.getAbsolutePath()) != MimeType.IMAGE) return new PackedMessage(null, MessageTaskReturnType.UNSUPPORTED_ATTACHMENTS);
            }
        }

        if (unprocessedMessage.getVoiceInput() != null){
            File inputFile = new File(unprocessedMessage.getVoiceInput());

            if (inputFile.exists()){
                if (AndroidUtils.getMimeTypeFromPath(inputFile.getAbsolutePath()) != MimeType.IMAGE) return new PackedMessage(null, MessageTaskReturnType.UNSUPPORTED_ATTACHMENTS);
            }
        }

        for (String s : unprocessedMessage.getInputAttachments()) {
            long length = new File(s).length();

            totalSize += length;
            sizes.add(length);
        }

        if (unprocessedMessage.getCameraInput() != null){
            File inputFile = new File(unprocessedMessage.getCameraInput());

            if (inputFile.exists()){
                totalSize += inputFile.length();
                sizes.add(inputFile.length());
            }
        }

        if (unprocessedMessage.getVoiceInput() != null){
            File inputFile = new File(unprocessedMessage.getVoiceInput());

            if (inputFile.exists()){
                totalSize += inputFile.length();
                sizes.add(inputFile.length());
            }
        }

        if (useMms){
            if (totalSize > weMessage.MAX_MMS_ATTACHMENT_SIZE * 7){
                return new PackedMessage(null, MessageTaskReturnType.FILE_SIZE_TOO_LARGE_MMS);
            }
        }

        if (totalSize > weMessage.MAX_FILE_SIZE){
            return new PackedMessage(null, MessageTaskReturnType.FILE_SIZE_TOO_LARGE);
        }

        if (sizes.size() > 0 && !AndroidUtils.hasMemoryForOperation(Collections.max(sizes))) {
            return new PackedMessage(null, MessageTaskReturnType.NOT_ENOUGH_MEMORY);
        }

        List<Attachment> attachments = new ArrayList<>();

        for (String s : unprocessedMessage.getInputAttachments()){
            try {
                String attachmentNamePrefix = new SimpleDateFormat("HH-mm-ss_MM-dd-yyyy", Locale.US).format(Calendar.getInstance().getTime());
                String transferName = Uri.parse(s).getLastPathSegment();
                File copiedFile = new File(weMessage.get().getAttachmentFolder(), attachmentNamePrefix + "-" + transferName);

                copiedFile.createNewFile();
                FileUtils.copy(new File(s), copiedFile);

                long totalBytes = copiedFile.length();
                Attachment a = new Attachment(
                        UUID.randomUUID(),
                        null,
                        transferName,
                        new FileLocationContainer(copiedFile),
                        AndroidUtils.getMimeTypeStringFromPath(s),
                        totalBytes
                );
                attachments.add(a);

            }catch (Exception ex){
                showErroredSnackbar(getString(R.string.send_attachment_error));
                AppLogger.error("An error occurred while loading a file into a message.", ex);
            }
        }

        if (unprocessedMessage.getCameraInput() != null){
            File inputFile = new File(unprocessedMessage.getCameraInput());

            if (inputFile.exists()) {
                long totalBytes = inputFile.length();
                Attachment a = new Attachment(
                        UUID.randomUUID(),
                        null,
                        Uri.parse(unprocessedMessage.getCameraInput()).getLastPathSegment(),
                        new FileLocationContainer(unprocessedMessage.getCameraInput()),
                        AndroidUtils.getMimeTypeStringFromPath(unprocessedMessage.getCameraInput()),
                        totalBytes
                );
                attachments.add(a);
            }
        }

        if (unprocessedMessage.getVoiceInput() != null){
            File inputFile = new File(unprocessedMessage.getVoiceInput());
            if (inputFile.exists()) {
                long totalBytes = inputFile.length();
                Attachment a = new Attachment(
                        UUID.randomUUID(),
                        null,
                        Uri.parse(unprocessedMessage.getVoiceInput()).getLastPathSegment(),
                        new FileLocationContainer(unprocessedMessage.getVoiceInput()),
                        AndroidUtils.getMimeTypeStringFromPath(unprocessedMessage.getVoiceInput()),
                        totalBytes
                );
                attachments.add(a);
            }
        }

        Message message;

        if (useMms){
            message = new MmsMessage(
                    null,
                    getChat(),
                    getChat() instanceof SmsChat ? weMessage.get().getCurrentSession().getSmsHandle() : weMessage.get().getCurrentSession().getAccount().getHandle(),
                    attachments,
                    unprocessedMessage.getInput().toString().trim(),
                    Calendar.getInstance().getTime(),
                    null,
                    false,
                    false,
                    true,
                    false,
                    true
            );
        }else {
            message = new Message(
                    UUID.randomUUID().toString(),
                    null,
                    getChat(),
                    weMessage.get().getCurrentSession().getAccount().getHandle(),
                    attachments,
                    unprocessedMessage.getInput().toString().trim(),
                    DateUtils.convertDateTo2001Time(Calendar.getInstance().getTime()),
                    null,
                    null,
                    false,
                    true,
                    false,
                    false,
                    true,
                    true,
                    false,
                    MessageEffect.NONE,
                    false
            );
        }

        return new PackedMessage(message, MessageTaskReturnType.TASK_PERFORMED);
    }

    private UnprocessedMessage preprocessMessage(CharSequence input){
        ArrayList<String> inputAttachments = new ArrayList<>();
        String cameraAttachment = cameraAttachmentInput;
        String voiceMessage = voiceMessageInput;

        if (isPopupFragmentOpen) {
            if (getAttachmentPopupFragment() != null) {
                inputAttachments.addAll(getAttachmentPopupFragment().getSelectedAttachments());
                cameraAttachment = getAttachmentPopupFragment().getCameraAttachmentFile();
                voiceMessage = getAttachmentPopupFragment().getAudioFile();

                getAttachmentPopupFragment().clearSelectedAttachments();
            }
            closeAttachmentPopupFragment(true);
        } else {
            inputAttachments.addAll(attachmentsInput);
        }

        UnprocessedMessage message = new UnprocessedMessage(input, inputAttachments, cameraAttachment, voiceMessage);

        attachmentsInput.clear();
        cameraAttachmentInput = null;
        voiceMessageInput = null;

        return message;
    }

    private void showDeliveryStatusOnLastMessage(){
        Message lastMessage = weMessage.get().getMessageDatabase().getLastMessageFromChat(getChat());

        if (lastMessage == null) return;

        String oldLastMessageId = lastMessageId;
        lastMessageId = lastMessage.getIdentifier();

        for (int childCount = messageList.getChildCount(), i = 0; i < childCount; ++i) {
            RecyclerView.ViewHolder holder = messageList.getChildViewHolder(messageList.getChildAt(i));

            if (holder instanceof OutgoingMessageViewHolder){
                OutgoingMessageViewHolder messageHolder = (OutgoingMessageViewHolder) holder;

                if (!StringUtils.isEmpty(oldLastMessageId) && messageHolder.getMessageId().equals(oldLastMessageId)){
                    messageHolder.toggleDeliveryVisibility(false);
                }

                if (messageHolder.getMessageId().equals(lastMessageId)){
                    messageHolder.toggleDeliveryVisibility(true);
                }
            }
        }
    }

    private void showMessageOptionsSheetView(final MessageView message){
        conversationBottomSheet.showWithSheetView(LayoutInflater.from(getActivity()).inflate(R.layout.sheet_conversation_message_options, conversationBottomSheet, false));

        if (message.hasErrored()){
            if (message.getMessage() instanceof MmsMessage){
                conversationBottomSheet.findViewById(R.id.conversationSheetRetryButton).setVisibility(View.GONE);
                conversationBottomSheet.findViewById(R.id.conversationSheetRetryDivider).setVisibility(View.GONE);
                conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsButton).setVisibility(View.VISIBLE);
                conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsDivider).setVisibility(View.VISIBLE);

                setSendAsSmsButton(message);
            }else {
                if (MmsManager.isPhone()){
                    conversationBottomSheet.findViewById(R.id.conversationSheetRetryButton).setVisibility(View.VISIBLE);
                    conversationBottomSheet.findViewById(R.id.conversationSheetRetryDivider).setVisibility(View.VISIBLE);
                    conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsButton).setVisibility(View.VISIBLE);
                    conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsDivider).setVisibility(View.VISIBLE);

                    setRetryButton(message);
                    setSendAsSmsButton(message);
                }else {
                    conversationBottomSheet.findViewById(R.id.conversationSheetRetryButton).setVisibility(View.VISIBLE);
                    conversationBottomSheet.findViewById(R.id.conversationSheetRetryDivider).setVisibility(View.VISIBLE);
                    conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsButton).setVisibility(View.GONE);
                    conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsDivider).setVisibility(View.GONE);

                    setRetryButton(message);
                }
            }
        } else {
            conversationBottomSheet.findViewById(R.id.conversationSheetRetryButton).setVisibility(View.GONE);
            conversationBottomSheet.findViewById(R.id.conversationSheetRetryDivider).setVisibility(View.GONE);
            conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsButton).setVisibility(View.GONE);
            conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsDivider).setVisibility(View.GONE);
        }

        conversationBottomSheet.findViewById(R.id.conversationSheetCopyButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (StringUtils.isEmpty(message.getText()) && message.getMessage().getAttachments().size() > 0){
                    Toast.makeText(getActivity(), getString(R.string.copy_message_attachments), Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayList<MessageView> messageList = new ArrayList<>();
                messageList.add(message);

                copyMessages(messageList);
                conversationBottomSheet.dismissSheet();
            }
        });

        conversationBottomSheet.findViewById(R.id.conversationSheetDeleteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                weMessage.get().getMessageManager().removeMessage(message.getMessage(), true);
                conversationBottomSheet.dismissSheet();
            }
        });

        conversationBottomSheet.findViewById(R.id.conversationSheetMoreButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSelectionMode(true);
                conversationBottomSheet.dismissSheet();
            }
        });
    }

    private void copyMessages(List<MessageView> messages){
        StringBuilder fullMessage = new StringBuilder("");

        Collections.sort(messages, new Comparator<MessageView>() {
            @Override
            public int compare(MessageView o1, MessageView o2) {
                return o1.getCreatedAt().compareTo(o2.getCreatedAt());
            }
        });

        boolean showDetails = false;
        String initSender = null;

        if (messages.size() == 1){
            showDetails = true;
        }else {
            for (MessageView message : messages) {
                if (initSender == null) {
                    initSender = message.getUser().getId();
                }

                if (!initSender.equals(message.getUser().getId())) {
                    showDetails = true;
                    break;
                }
            }
        }

        for (MessageView message : messages){
            if (!StringUtils.isEmpty(message.getText())) {
                if (!showDetails){
                    fullMessage.append(message.getText()).append("\n");
                    continue;
                }

                String createdAt;

                if (DateFormatter.isCurrentYear(message.getCreatedAt())) {
                    createdAt = new SimpleDateFormat("EEEE, MMMM d 'at' h:mm a", Locale.getDefault()).format(message.getCreatedAt());
                } else {
                    createdAt = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(message.getCreatedAt());
                }

                fullMessage.append(String.format(Locale.getDefault(), "%s: %s (%s)", message.getUser().getName(), message.getText(), createdAt)).append("\n");
            }
        }

        String finalMessage = fullMessage.toString();

        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(finalMessage, finalMessage);
        clipboard.setPrimaryClip(clip);

        if (messages.size() == 1) {
            showPositiveSnackbar(3, getString(R.string.copy_message_success_single));
        }else {
            showPositiveSnackbar(3, getString(R.string.copy_message_success_multiple));
        }
    }

    private void toggleSelectionMode(boolean value){
        if (isSelectionMode != value) {
            isSelectionMode = value;

            if (chat.isInChat()) {
                if (value) {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bottomDivider.getLayoutParams();

                    layoutParams.removeRule(RelativeLayout.ABOVE);
                    layoutParams.addRule(RelativeLayout.ABOVE, R.id.messageSelectionModeBar);

                    bottomDivider.setLayoutParams(layoutParams);
                    messageInput.setVisibility(View.GONE);
                    messageSelectionModeBar.setVisibility(View.VISIBLE);
                } else {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bottomDivider.getLayoutParams();

                    layoutParams.removeRule(RelativeLayout.ABOVE);
                    layoutParams.addRule(RelativeLayout.ABOVE, R.id.messageInputView);

                    bottomDivider.setLayoutParams(layoutParams);
                    messageSelectionModeBar.setVisibility(View.GONE);
                    messageInput.setVisibility(View.VISIBLE);
                }
            } else {
                if (value) {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) messageList.getLayoutParams();

                    layoutParams.removeRule(RelativeLayout.ABOVE);
                    layoutParams.addRule(RelativeLayout.ABOVE, R.id.messageSelectionModeBar);

                    messageList.setLayoutParams(layoutParams);
                    messageSelectionModeBar.setVisibility(View.VISIBLE);
                } else {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) messageList.getLayoutParams();

                    layoutParams.removeRule(RelativeLayout.ABOVE);

                    messageList.setLayoutParams(layoutParams);
                    messageSelectionModeBar.setVisibility(View.GONE);
                }
            }

            if (!value){
                selectedMessages.clear();
            }

            for (int childCount = messageList.getChildCount(), i = 0; i < childCount; ++i) {
                RecyclerView.ViewHolder holder = messageList.getChildViewHolder(messageList.getChildAt(i));

                if (holder instanceof MessageViewHolder){
                    MessageViewHolder messageHolder = (MessageViewHolder) holder;

                    if (!value){
                        messageHolder.setSelected(false);
                    }

                    messageHolder.toggleSelectionMode(value);
                }
            }
        }
    }

    private void setRetryButton(final MessageView message){
        conversationBottomSheet.findViewById(R.id.conversationSheetRetryButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conversationBottomSheet.dismissSheet();
                if (showOfflineModes()) return;

                Message newMessage = new Message(UUID.randomUUID().toString(), null, getChat(),
                        weMessage.get().getMessageDatabase().getHandleByAccount(weMessage.get().getCurrentSession().getAccount()),
                        message.getMessage().getAttachments(), message.getMessage().getText(),
                        DateUtils.convertDateTo2001Time(Calendar.getInstance().getTime()),
                        null, null, false, true, false,
                        false, true, true, false, MessageEffect.NONE, false
                );
                serviceConnection.getConnectionService().getConnectionHandler().sendOutgoingMessage(newMessage, true);
            }
        });
    }

    private void setSendAsSmsButton(final MessageView message){
        conversationBottomSheet.findViewById(R.id.conversationSheetSendAsSmsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conversationBottomSheet.dismissSheet();

                if (!MmsManager.isDefaultSmsApp()){
                    DialogDisplayer.generateAlertDialog(getString(R.string.sms_error), getString(R.string.send_message_sms_not_default)).show(getFragmentManager(), "SendSmsErrorAlert");
                    return;
                }

                MmsMessage mmsMessage = new MmsMessage(null, getChat(),
                        weMessage.get().getCurrentSession().getSmsHandle(), message.getMessage().getAttachments(), message.getMessage().getText(),
                        Calendar.getInstance().getTime(), null, false, false, true, false,true
                );
                weMessage.get().getMmsManager().sendMessage(mmsMessage);
            }
        });
    }

    private void toggleIsInChat(boolean value){
        toggleSelectionMode(false);

        if (value){
            if (messageInput.getVisibility() != View.VISIBLE) {
                messageInput.setVisibility(View.VISIBLE);
                bottomDivider.setVisibility(View.VISIBLE);

                RelativeLayout.LayoutParams messageListLayoutParams = (RelativeLayout.LayoutParams) messageList.getLayoutParams();
                RelativeLayout.LayoutParams galleryFragmentLayoutParams = (RelativeLayout.LayoutParams) galleryFragmentContainer.getLayoutParams();

                messageListLayoutParams.addRule(RelativeLayout.ABOVE, R.id.messageInputDivider);
                galleryFragmentLayoutParams.removeRule(RelativeLayout.BELOW);
                galleryFragmentLayoutParams.addRule(RelativeLayout.BELOW, R.id.messageInputView);

                messageList.setLayoutParams(messageListLayoutParams);
                galleryFragmentContainer.setLayoutParams(galleryFragmentLayoutParams);
            }
        }else {
            if (messageInput.getVisibility() != View.GONE) {
                messageInput.setVisibility(View.GONE);
                bottomDivider.setVisibility(View.GONE);

                RelativeLayout.LayoutParams messageListLayoutParams = (RelativeLayout.LayoutParams) messageList.getLayoutParams();
                RelativeLayout.LayoutParams galleryFragmentLayoutParams = (RelativeLayout.LayoutParams) galleryFragmentContainer.getLayoutParams();

                messageListLayoutParams.removeRule(RelativeLayout.ABOVE);
                galleryFragmentLayoutParams.removeRule(RelativeLayout.BELOW);
                galleryFragmentLayoutParams.addRule(RelativeLayout.BELOW, R.id.messagesList);

                messageList.setLayoutParams(messageListLayoutParams);
                galleryFragmentContainer.setLayoutParams(galleryFragmentLayoutParams);
            }
        }
    }

    private void saveMediaToGallery(Attachment a){
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getString(R.string.no_media_write_permission), "WritePermissionAlertFragment", weMessage.REQUEST_PERMISSION_WRITE_STORAGE)){
            tempPermissionDownloadAttachment = a.getUuid().toString();
            return;
        }
        IOUtils.saveMediaToGallery(this, getActivity(), getView(), AndroidUtils.getMimeTypeFromPath(a.getFileLocation().getFileLocation()), IOUtils.getUriFromFile(a.getFileLocation().getFile()).toString());
    }

    private void launchContactView(){
        Intent launcherIntent = new Intent(weMessage.get(), ContactViewActivity.class);

        launcherIntent.putExtra(weMessage.BUNDLE_CONTACT_VIEW_UUID, ((PeerChat) getChat()).getHandle().getUuid().toString());
        launcherIntent.putExtra(weMessage.BUNDLE_CONVERSATION_CHAT, getChat().getIdentifier());

        startActivity(launcherIntent);
        getActivity().finish();
    }

    private void launchChatView(){
        Intent launcherIntent = new Intent(weMessage.get(), ChatViewActivity.class);
        launcherIntent.putExtra(weMessage.BUNDLE_CONVERSATION_CHAT, getChat().getIdentifier());

        startActivity(launcherIntent);
        getActivity().finish();
    }

    private void launchConversationView(String chatId){
        Intent launcherIntent = new Intent(weMessage.get(), ConversationActivity.class);

        launcherIntent.putExtra(weMessage.BUNDLE_RETURN_POINT, ChatListActivity.class.getName());
        launcherIntent.putExtra(weMessage.BUNDLE_CONVERSATION_CHAT, chatId);

        startActivity(launcherIntent);
        getActivity().finish();
    }

    private void launchFullScreenImageActivity(String imageUri){
        Intent launcherIntent = new Intent(weMessage.get(), MessageImageActivity.class);

        launcherIntent.putExtra(weMessage.BUNDLE_FULL_SCREEN_IMAGE_URI, imageUri);
        launcherIntent.putExtra(weMessage.BUNDLE_CONVERSATION_CHAT, getChat().getIdentifier());

        startActivity(launcherIntent);
        getActivity().finish();
    }

    private void launchFullScreenVideoActivity(String imageUri){
        Intent launcherIntent = new Intent(weMessage.get(), MessageVideoActivity.class);

        launcherIntent.putExtra(weMessage.BUNDLE_FULL_SCREEN_VIDEO_URI, imageUri);
        launcherIntent.putExtra(weMessage.BUNDLE_CONVERSATION_CHAT, getChat().getIdentifier());

        startActivity(launcherIntent);
        getActivity().finish();
    }

    private void launchAttachmentPopupFragment(boolean performAnimation){
        if (!isPopupFragmentOpen) {
            isPopupFragmentOpen = true;

            if (performAnimation) {
                galleryFragmentContainer.animate().alpha(1.0f).translationY(0).setDuration(250).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);

                        handleOpenAttachmentPopupFragment();
                    }
                });
            } else {
                handleOpenAttachmentPopupFragment();
            }
        }
    }

    private void launchAttachmentPopupFragmentWithCameraIntent(int resultCode, Intent data){
        isPopupFragmentOpen = true;

        RelativeLayout.LayoutParams messageInputLayoutParams = (RelativeLayout.LayoutParams) messageInput.getLayoutParams();
        RelativeLayout.LayoutParams fragmentContainerLayoutParams = (RelativeLayout.LayoutParams) galleryFragmentContainer.getLayoutParams();

        messageInputLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        messageInputLayoutParams.addRule(RelativeLayout.ABOVE, R.id.galleryFragmentContainer);
        fragmentContainerLayoutParams.removeRule(RelativeLayout.BELOW);
        fragmentContainerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        messageInput.setLayoutParams(messageInputLayoutParams);
        galleryFragmentContainer.setLayoutParams(fragmentContainerLayoutParams);
        galleryFragmentContainer.setVisibility(View.VISIBLE);

        AttachmentPopupFragment popupFragment = new AttachmentPopupFragment();
        Bundle popupArgs = new Bundle();

        popupArgs.putString(weMessage.ARG_CAMERA_ATTACHMENT_FILE, cameraAttachmentInput);
        popupArgs.putString(weMessage.ARG_VOICE_RECORDING_FILE, voiceMessageInput);
        popupArgs.putInt(weMessage.ARG_ATTACHMENT_POPUP_CAMERA_RESULT_CODE, resultCode);
        popupArgs.putParcelable(weMessage.ARG_ATTACHMENT_POPUP_CAMERA_INTENT, data);
        popupArgs.putStringArrayList(weMessage.ARG_ATTACHMENT_GALLERY_CACHE, new ArrayList<>(attachmentsInput));
        popupFragment.setArguments(popupArgs);

        getChildFragmentManager().beginTransaction().add(R.id.galleryFragmentContainer, popupFragment).commit();
    }

    private void closeAttachmentPopupFragment(boolean performAnimation){
        if (isPopupFragmentOpen) {
            isPopupFragmentOpen = false;

            if (performAnimation) {
                int height = galleryFragmentContainer.getHeight();

                galleryFragmentContainer.animate().alpha(0.f).translationY(height).setDuration(250).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        handleCloseAttachmentPopupFragment();
                    }
                });
            } else {
                handleCloseAttachmentPopupFragment();
            }
        }
    }

    private void handleOpenAttachmentPopupFragment(){
        RelativeLayout.LayoutParams messageInputLayoutParams = (RelativeLayout.LayoutParams) messageInput.getLayoutParams();
        RelativeLayout.LayoutParams fragmentContainerLayoutParams = (RelativeLayout.LayoutParams) galleryFragmentContainer.getLayoutParams();

        messageInputLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        messageInputLayoutParams.addRule(RelativeLayout.ABOVE, R.id.galleryFragmentContainer);
        fragmentContainerLayoutParams.removeRule(RelativeLayout.BELOW);
        fragmentContainerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        messageInput.setLayoutParams(messageInputLayoutParams);
        galleryFragmentContainer.setLayoutParams(fragmentContainerLayoutParams);
        galleryFragmentContainer.setVisibility(View.VISIBLE);

        AttachmentPopupFragment popupFragment = new AttachmentPopupFragment();
        Bundle popupArgs = new Bundle();

        popupArgs.putString(weMessage.ARG_CAMERA_ATTACHMENT_FILE, cameraAttachmentInput);
        popupArgs.putString(weMessage.ARG_VOICE_RECORDING_FILE, voiceMessageInput);
        popupArgs.putStringArrayList(weMessage.ARG_ATTACHMENT_GALLERY_CACHE, new ArrayList<>(attachmentsInput));
        popupFragment.setArguments(popupArgs);

        getChildFragmentManager().beginTransaction().add(R.id.galleryFragmentContainer, popupFragment).commit();
    }

    private void handleCloseAttachmentPopupFragment(){
        RelativeLayout.LayoutParams messageInputLayoutParams = (RelativeLayout.LayoutParams) messageInput.getLayoutParams();
        RelativeLayout.LayoutParams fragmentContainerLayoutParams = (RelativeLayout.LayoutParams) galleryFragmentContainer.getLayoutParams();

        fragmentContainerLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        fragmentContainerLayoutParams.addRule(RelativeLayout.BELOW, R.id.messageInput);
        messageInputLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        messageInputLayoutParams.removeRule(RelativeLayout.ABOVE);

        messageInput.setLayoutParams(messageInputLayoutParams);
        galleryFragmentContainer.setLayoutParams(fragmentContainerLayoutParams);
        galleryFragmentContainer.setVisibility(View.GONE);

        getChildFragmentManager().beginTransaction().remove(getChildFragmentManager().findFragmentById(R.id.galleryFragmentContainer)).commit();
    }

    private boolean isMessageBlocked(Message message){
        return message.getSender().isBlocked();
    }

    private boolean hasPermission(final String permission, String rationaleString, String alertTagId, final int requestCode){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(permission)){
                DialogDisplayer.AlertDialogFragment alertDialogFragment = DialogDisplayer.generateAlertDialog(getString(R.string.permissions_error_title), rationaleString);

                alertDialogFragment.setOnDismiss(new Runnable() {
                    @Override
                    public void run() {
                        requestPermissions(new String[] { permission }, requestCode);
                    }
                });
                alertDialogFragment.show(getFragmentManager(), alertTagId);
                return false;
            } else {
                requestPermissions(new String[] { permission }, requestCode);
                return false;
            }
        }
        return true;
    }

    private boolean isGranted(int[] grantResults){
        return (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
    }

    private void showDisconnectReasonDialog(Intent bundledIntent, String defaultMessage, Runnable runnable){
        DialogDisplayer.showDisconnectReasonDialog(getContext(), getFragmentManager(), bundledIntent, defaultMessage, runnable);
    }

    private void clearEditText(final EditText editText, boolean closeKeyboard){
        if (closeKeyboard) {
            closeKeyboard();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                editText.clearFocus();
            }
        }, 100);
    }

    private void closeKeyboard(){
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (getActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showErroredSnackbar(final String message){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getView() != null) {
                    final Snackbar snackbar = Snackbar.make(getView(), message, ERROR_SNACKBAR_DURATION * 1000);

                    snackbar.setAction(getString(R.string.dismiss_button), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackbar.dismiss();
                        }
                    });
                    snackbar.setActionTextColor(getResources().getColor(R.color.brightRedText));

                    View snackbarView = snackbar.getView();
                    TextView textView = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setMaxLines(5);

                    snackbar.show();
                }
            }
        });
    }

    private void showPositiveSnackbar(final int duration, final String message){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Snackbar snackbar = Snackbar.make(getView(), message, duration * 1000);

                snackbar.setAction(getString(R.string.ok_button), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackbar.dismiss();
                    }
                });
                snackbar.setActionTextColor(getResources().getColor(R.color.colorHeader));

                View snackbarView = snackbar.getView();
                TextView textView = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setMaxLines(5);

                snackbar.show();
            }
        });
    }

    private boolean showOfflineModes(){
        if (isStillConnecting()){
            showErroredSnackbar(getString(R.string.still_connecting_send_message));
            return true;
        }else if (!isConnectionServiceRunning()){
            DialogDisplayer.AlertDialogFragmentDouble alertDialogFragment =
                    DialogDisplayer.generateOfflineDialog(getActivity(), getString(R.string.offline_mode_message_send, MmsManager.isDefaultSmsApp() ? getString(R.string.sms_mode) : getString(R.string.offline_mode)));

            alertDialogFragment.setOnDismiss(new Runnable() {
                @Override
                public void run() {
                    LaunchActivity.launchActivity(getActivity(), ConversationFragment.this, true);
                }
            });

            alertDialogFragment.show(getFragmentManager(), "OfflineModeAlertDialog");
            return true;
        }
        return false;
    }

    private boolean isChatThis(Chat c){
        try {
            return c.getIdentifier().equals(getChat().getIdentifier());
        }catch (Exception ex){
            return false;
        }
    }

    private boolean isPossibleSmsGroupChat(Chat chat){
        GroupChat groupChat = (GroupChat) chat;

        for (Handle h : groupChat.getParticipants()){
            if (!PhoneNumberUtil.getInstance().isPossibleNumber(h.getHandleID(), Resources.getSystem().getConfiguration().locale.getCountry())) return false;
        }

        return true;
    }

    private boolean isStillConnecting(){
        return serviceConnection.getConnectionService() == null || !serviceConnection.getConnectionService().getConnectionHandler().isConnected().get();
    }

    private String parseURL(String urlString){
        if (!urlString.toLowerCase().startsWith("http://") && !urlString.toLowerCase().startsWith("https://")){
            String https = "https://" + urlString;

            try {
                URL url = new URL(urlString);

                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();

                inputStream.close();
                urlConnection.disconnect();

                return https;
            }catch (Exception ex){
                return "http://" + urlString;
            }
        }else {
            return urlString;
        }
    }

    public void goToChatList(String reason){
        if (isAdded() || (getActivity() != null && !getActivity().isFinishing())) {
            Intent returnIntent = new Intent(weMessage.get(), ChatListActivity.class);

            if (reason != null) {
                returnIntent.putExtra(weMessage.BUNDLE_CONVERSATION_GO_BACK_REASON, reason);
            }

            startActivity(returnIntent);
            getActivity().finish();
        }
    }

    private Drawable getSelector(@ColorInt int normalColor, @ColorInt int pressedColor, @ColorInt int disabledColor, @DrawableRes int shape) {
        Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), shape)).mutate();
        DrawableCompat.setTintList(drawable,
                new ColorStateList(
                        new int[][]{
                                new int[]{android.R.attr.state_enabled, -android.R.attr.state_pressed},
                                new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed},
                                new int[]{-android.R.attr.state_enabled}
                        },
                        new int[]{normalColor, pressedColor, disabledColor}
                ));
        return drawable;
    }

    private class UnprocessedMessage {
        private CharSequence input;
        private ArrayList<String> inputAttachments;
        private String cameraInput;
        private String voiceInput;

        UnprocessedMessage(CharSequence input, ArrayList<String> inputAttachments, String cameraInput, String voiceInput){
           this.input = input;
           this.inputAttachments = inputAttachments;
           this.cameraInput = cameraInput;
           this.voiceInput = voiceInput;
       }

        CharSequence getInput() {
            return input;
        }

        ArrayList<String> getInputAttachments() {
            return inputAttachments;
        }

        String getCameraInput() {
            return cameraInput;
        }

        String getVoiceInput() {
            return voiceInput;
        }

        boolean hasNoAttachments(){
            return getInputAttachments().isEmpty() && getCameraInput() == null && getVoiceInput() == null;
        }
    }

    private class PackedMessage {
        private Message message;
        private MessageTaskReturnType messageTaskReturnType;

        PackedMessage(Message message, MessageTaskReturnType messageTaskReturnType) {
            this.message = message;
            this.messageTaskReturnType = messageTaskReturnType;
        }

        Message getMessage() {
            return message;
        }

        MessageTaskReturnType getMessageTaskReturnType() {
            return messageTaskReturnType;
        }
    }

    private enum MessageTaskReturnType {
        FILE_SIZE_TOO_LARGE,
        FILE_SIZE_TOO_LARGE_MMS,
        NOT_ENOUGH_MEMORY,
        TASK_PERFORMED,
        UNSUPPORTED_ATTACHMENTS
    }
}