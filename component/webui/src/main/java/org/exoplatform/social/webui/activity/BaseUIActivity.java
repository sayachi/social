/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.webui.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.Activity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormTextAreaInput;

/**
 * UIActivity.java
 * <p>
 * Displays activity
 * </p>
 * @author    <a href="http://hoatle.net">hoatle</a>
 * @since     Apr 12, 2010
 * @copyright eXo Platform SAS
 */
public class BaseUIActivity extends UIForm {
  static private final Log LOG = ExoLogger.getLogger(BaseUIActivity.class);

  static public int LATEST_COMMENTS_SIZE = 2;
  private int commentMinCharactersAllowed = 0;
  private int commentMaxCharactersAllowed = 100;

  static public enum CommentStatus {
    LATEST("latest"),    ALL("all"),    NONE("none");
    public String getStatus() {
      return commentStatus;
    }
    private CommentStatus(String status) {
      commentStatus = status;
    }
    private String commentStatus;
  }

  private String title;
  private String image;
  private Activity activity;
  private List<Activity> comments;
  private String[] identityLikes;
  private ActivityManager activityManager;
  private IdentityManager identityManager;
  private boolean commentFormDisplayed = false;
  private boolean likesDisplayed = false;
  private CommentStatus commentListStatus = CommentStatus.LATEST;
  private boolean allCommentsHidden = false;
  private boolean commentFormFocused = false;
  private UIFormTextAreaInput commentInput;
  /**
   * Constructor
   * @throws Exception
   */
  public BaseUIActivity(){
    //tricktip for gatein bug
    setSubmitAction("return false;");
  }

  public void setActivity(Activity activity) {
    this.activity = activity;
    addChild(new UIFormTextAreaInput("CommentTextarea" + activity.getId(), "CommentTextarea", null));
    refresh();
  }

  public Activity getActivity() {
    return activity;
  }

  public void setCommentMinCharactersAllowed(int num) {
    commentMinCharactersAllowed = num;
  }

  public int getCommentMinCharactersAllowed() {
    return commentMinCharactersAllowed;
  }

  public void setCommentMaxCharactersAllowed(int num) {
    commentMaxCharactersAllowed = num;
  }

  public int getCommentMaxCharactersAllowed() {
    return commentMaxCharactersAllowed;
  }

  public void setCommentFormDisplayed(boolean commentFormDisplayed) {
    this.commentFormDisplayed = commentFormDisplayed;
  }

  public boolean isCommentFormDisplayed() {
    return commentFormDisplayed;
  }

  public void setLikesDisplayed(boolean likesDisplayed) {
    this.likesDisplayed = likesDisplayed;
  }

  public boolean isLikesDisplayed() {
    return likesDisplayed;
  }

  public void setAllCommentsHidden(boolean allCommentsHidden) {
    this.allCommentsHidden = allCommentsHidden;
  }

  public boolean isAllCommentsHidden() {
    return allCommentsHidden;
  }

  public void setCommentFormFocused(boolean commentFormFocused) {
    this.commentFormFocused = commentFormFocused;
  }

  public boolean isCommentFormFocused() {
    return commentFormFocused;
  }

  public void setCommentListStatus(CommentStatus status) {
    commentListStatus = status;
    if (status == CommentStatus.ALL) {
      commentFormDisplayed = true;
    }
  }

  public CommentStatus getCommentListStatus() {
    return commentListStatus;
  }

  public boolean commentListToggleable() {
    return comments.size() > LATEST_COMMENTS_SIZE;
  }

  /**
   * Gets all the comments or latest comments or empty list comments
   * Gets latest comments for displaying at the first time
   * if available, returns max LATEST_COMMENTS_SIZE latest comments.
   * @return
   */
  public List<Activity> getComments() {
    if (commentListStatus == CommentStatus.ALL) {
      return comments;
    } else if (commentListStatus == CommentStatus.NONE) {
      return new ArrayList<Activity>();
    } else {
      int commentsSize = comments.size();
      if (commentsSize > LATEST_COMMENTS_SIZE) {
        return comments.subList(commentsSize - LATEST_COMMENTS_SIZE, commentsSize);
      }
    }
    return comments;
  }

  public List<Activity> getAllComments() {
    return comments;
  }

  public String[] getIdentityLikes() {
    return identityLikes;
  }

  /**
   * removes currently viewing userId if he liked this activity
   * @return
   * @throws Exception
   */
  public String[] getDisplayedIdentityLikes() throws Exception {
    identityManager = getIdentityManager();
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, getRemoteUser());
    if (isLiked()) {
      return (String[]) ArrayUtils.removeElement(identityLikes, userIdentity.getId());
    }
    return identityLikes;
  }

  public void setIdenityLikes(String[] identityLikes) {
    this.identityLikes = identityLikes;
  }

  public void setImage(String image) {
    this.image = image;
  }

  /**
   * Gets activity image
   *
   * @return
   */
  public String getImage (){
    return image;
  }

  public String event(String actionName, String callback, boolean updateForm) throws Exception {
    if (updateForm) {
      return super.url(actionName);
    }
    StringBuilder b = new StringBuilder();
    b.append("javascript:eXo.social.webui.UIForm.submitForm('").append(getFormId()).append("','");
    b.append(actionName).append("',");
    b.append(callback).append(",");
    b.append("true").append(")");
    return b.toString();
  }

  /**
   * Gets prettyTime by timestamp
   * @param postedTime
   * @return
   */
  public String getPostedTimeString(long postedTime) {
    //TODO use app resource
    long time = (new Date().getTime() - postedTime) / 1000;
    long value = 0;
    if (time < 60) {
      return "less than a minute ago";
    } else {
      if (time < 120) {
        return "about a minute ago";
      } else {
        if (time < 3600) {
          value = Math.round(time / 60);
          return "about " + value + " minutes ago";
        } else {
          if (time < 7200) {
            return "about an hour ago";
          } else {
            if (time < 86400) {
              value = Math.round(time / 3600);
              return "about " + value + " hours ago";
            } else {
              if (time < 172800) {
                return "about a day ago";
              } else {
                if (time < 2592000) {
                  value = Math.round(time / 86400);
                  return "about " + value + " days ago";
                } else {
                  if (time < 5184000) {
                    return "about a month ago";
                  } else {
                    value = Math.round(time / 2592000);
                    return "about " + value + " months ago";
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private String getFormId() {
     WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
     if (context instanceof PortletRequestContext) {
        return ((PortletRequestContext)context).getWindowId() + "#" + getId();
     }
     return getId();
  }

  protected void saveComment(String remoteUser, String message) throws Exception {
    activityManager = getActivityManager();
    identityManager = getIdentityManager();
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteUser);
    Activity comment = new Activity(userIdentity.getId(), SpaceService.SPACES_APP_ID, message, null);
    activityManager.saveComment(getActivity(), comment);
    comments = activityManager.getComments(getActivity());
    setCommentListStatus(CommentStatus.ALL);
  }

  protected void setLike(boolean isLiked, String remoteUser) throws Exception {
    activityManager = getActivityManager();
    identityManager = getIdentityManager();
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteUser);
    if (isLiked) {
      activityManager.saveLike(activity, userIdentity);
    } else {
      activityManager.removeLike(activity, userIdentity);
    }
    activity = activityManager.getActivity(activity.getId());
    setIdenityLikes(activity.getLikeIdentityIds());
  }

  /**
   * Checks if this activity is liked by the remote user
   * @return
   * @throws Exception
   */
  public boolean isLiked() throws Exception {
    identityManager = getIdentityManager();
    return ArrayUtils.contains(identityLikes, identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, getRemoteUser()).getId());
  }

  /**
   * refresh, regets all like, comments of this activity
   */
  protected void refresh() {
    activityManager = getActivityManager();
    activity = activityManager.getActivity(activity.getId());
    if (activity == null) { //not found -> should render nothing
      LOG.info("activity_ is null, not found. It can be deleted!");
      return;
    }
    comments = activityManager.getComments(activity);
    identityLikes = activity.getLikeIdentityIds();
  }


  private String getRemoteUser() {
    PortalRequestContext requestContext = Util.getPortalRequestContext();
    return requestContext.getRemoteUser();
  }

  public String getUserFullName(String userIdentityId) throws Exception {
    Identity userIdentity = getIdentityManager().getIdentity(userIdentityId, true);
    if (userIdentity == null) {
      return null;
    }
    Profile userProfile = userIdentity.getProfile();
    return userProfile.getFullName();
  }

  /**
   * Gets user profile uri
   * @param userIdentityId
   * @return
   * @throws Exception
   */
  public String getUserProfileUri(String userIdentityId) {
    try {
      Identity userIdentity = getIdentityManager().getIdentity(userIdentityId, true);
      if (userIdentity == null) {
        return "#";
      }

      String url = userIdentity.getProfile().getUrl();
      if (url != null) {
        return url;
      } else {
        return "#";
      }
    } catch (Exception e) {
      return "#";
    }
  }

  /**
   * Gets user's avatar image source by userIdentityId
   * @param userIdentityId
   * @return
   * @throws Exception
   */
  public String getUserAvatarImageSource(String userIdentityId) throws Exception {
    Identity userIdentity = identityManager.getIdentity(userIdentityId, true);
    if (userIdentity == null) {
      return null;
    }
    Profile userProfile = userIdentity.getProfile();
    return userProfile.getAvatarImageSource();
  }

  /**
   * Gets activityManager
   * @return
   */
  private ActivityManager getActivityManager() {
    return getApplicationComponent(ActivityManager.class);
  }

  /**
   * Gets identityManager
   * @return
   */
  private IdentityManager getIdentityManager() {
    return getApplicationComponent(IdentityManager.class);
  }

  public boolean isSpaceActivity(String id) {
    try {
      identityManager = getIdentityManager();
      Identity identity = identityManager.getIdentity(id, false);
      String remoteId = identity.getRemoteId();
      boolean result = (identityManager.getIdentity(SpaceIdentityProvider.NAME, remoteId, false) != null);
      return result;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isUserActivity(String id) throws Exception {
    try {
      identityManager = getIdentityManager();
      Identity identity = identityManager.getIdentity(id, false);
      String remoteId = identity.getRemoteId();
      boolean result = (identityManager.getIdentity(OrganizationIdentityProvider.NAME,
                                                     remoteId,
                                                     false) != null);
      return result;
    } catch (Exception e) {
      return false;
    }
  }

    static public class ToggleDisplayLikesActionListener extends EventListener<BaseUIActivity> {
    @Override
    public void execute(Event<BaseUIActivity> event) throws Exception {
      BaseUIActivity uiActivity = event.getSource();
      uiActivity.refresh();
      if (uiActivity.isLikesDisplayed()) {
        uiActivity.setLikesDisplayed(false);
      } else {
        uiActivity.setLikesDisplayed(true);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActivity);
    }
  }

  public static class LikeActivityActionListener extends EventListener<BaseUIActivity> {
    @Override
    public void execute(Event<BaseUIActivity> event) throws Exception {
      BaseUIActivity uiActivity = event.getSource();
      uiActivity.refresh();
      WebuiRequestContext requestContext = event.getRequestContext();
      String isLikedStr = requestContext.getRequestParameter(OBJECTID);
      boolean isLiked = Boolean.parseBoolean(isLikedStr);
      uiActivity.setLike(isLiked, requestContext.getRemoteUser());
      requestContext.addUIComponentToUpdateByAjax(uiActivity);
    }
  }

  public static class SetCommentListStatusActionListener extends EventListener<BaseUIActivity> {
    @Override
    public void execute(Event<BaseUIActivity> event) throws Exception {
      BaseUIActivity uiActivity = event.getSource();
      uiActivity.refresh();
      String status = event.getRequestContext().getRequestParameter(OBJECTID);
      CommentStatus commentListStatus = null;
      if (status.equals(CommentStatus.LATEST.getStatus())) {
        commentListStatus = CommentStatus.LATEST;
      } else if (status.equals(CommentStatus.ALL.getStatus())) {
        commentListStatus = CommentStatus.ALL;
      } else if (status.equals(CommentStatus.NONE.getStatus())) {
        commentListStatus = CommentStatus.NONE;
      }
      if (commentListStatus != null) {
        uiActivity.setCommentListStatus(commentListStatus);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActivity);
    }
  }

  public static class ToggleDisplayCommentFormActionListener extends EventListener<BaseUIActivity> {
    @Override
    public void execute(Event<BaseUIActivity> event) throws Exception {
      BaseUIActivity uiActivity = event.getSource();
      if (uiActivity.isCommentFormDisplayed()) {
        uiActivity.setCommentFormDisplayed(false);
      } else {
        uiActivity.setCommentFormDisplayed(true);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActivity);
    }
  }

  public static class PostCommentActionListener extends EventListener<BaseUIActivity> {
    @Override
    public void execute(Event<BaseUIActivity> event) throws Exception {
      BaseUIActivity uiActivity = event.getSource();
      uiActivity.refresh();
      WebuiRequestContext requestContext = event.getRequestContext();
      UIFormTextAreaInput uiFormComment = uiActivity.getChild(UIFormTextAreaInput.class);
      String message = uiFormComment.getValue();
      uiFormComment.reset();
      uiActivity.saveComment(requestContext.getRemoteUser(), message);
      uiActivity.setCommentFormFocused(true);
      requestContext.addUIComponentToUpdateByAjax(uiActivity);

      uiActivity.getParent().broadcast(event, event.getExecutionPhase());
    }
  }
}