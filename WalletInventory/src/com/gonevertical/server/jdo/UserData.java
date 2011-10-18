package com.gonevertical.server.jdo;

import java.util.Iterator;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Version;
import javax.jdo.annotations.VersionStrategy;

import com.gonevertical.server.PMF;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@PersistenceCapable
@Version(strategy=VersionStrategy.VERSION_NUMBER, column="version", extensions={@Extension(vendorName="datanucleus", key="key", value="version")})
public class UserData {

  public static PersistenceManager getPersistenceManager() {
    return PMF.get().getPersistenceManager();
  }

  private static User getGoogleUser() {
    UserService userService = UserServiceFactory.getUserService();
    if (userService.isUserLoggedIn() == false) {
      return null;
    }
    User user = userService.getCurrentUser();
    return user;
  }

  public static Long getLoggedInUserId() {
    User user = getGoogleUser();
    if (user == null) {
      return null;
    }
    UserData userData = findUserDataByGoogleUserId(user.getUserId());
    if (userData == null) {
      return null;
    }
    if (userData.getId() == null) {
      return null;
    }
    return userData.getId();
  }

  public static UserData findUserData(Long id) {
    if (id == null) {
      return null;
    }
    PersistenceManager pm = getPersistenceManager();
    try {
      UserData e = pm.getObjectById(UserData.class, id);
      return e;
    } finally {
      pm.close();
    }
  }

  public static UserData findUserDataByGoogleEmail(String googleEmail) {
    PersistenceManager pm = getPersistenceManager();
    try {
      javax.jdo.Query query = pm.newQuery("select from " + UserData.class.getName());
      query.setRange(0, 1);
      query.setFilter("googleEmail==\""+ googleEmail + "\"");
      List<UserData> list = (List<UserData>) query.execute();
      long size = list.size();
      if (size == 0) {
        return null;
      }
      Iterator<UserData> itr = list.iterator();
      UserData ud = itr.next();
      return ud;
    } finally {
      pm.close();
    }
  }

  public static UserData findUserDataByGoogleUserId(String googleUserId) {
    PersistenceManager pm = getPersistenceManager();
    try {
      javax.jdo.Query query = pm.newQuery("select from " + UserData.class.getName());
      query.setRange(0, 1);
      query.setFilter("googleUserId==\"" + googleUserId + "\"");
      List<UserData> list = (List<UserData>) query.execute();
      long size = list.size();
      if (size == 0) {
        return null;
      }
      Iterator<UserData> itr = list.iterator();
      UserData ud = itr.next();
      return ud;
    } finally {
      pm.close();
    }
  }



  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;

  @Persistent
  private Long version;

  @Persistent
  private String googleUserId;

  @Persistent
  private String googleEmail;

  @Persistent
  private String googleNickname;

  @NotPersistent
  private String loginUrl;

  @NotPersistent
  private String logoutUrl;


  public void setId(Long id) {
    if (id == null) {
      return;
    }
    this.key = KeyFactory.createKey(UserData.class.getName(), id);
  }
  public Long getId() {
    Long id = null;
    if (key != null) {
      id = key.getId();
    }
    return id;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
  public Long getVersion() {
    return version;
  }

  public void setGoogleUserId(String googleUserId) {
    this.googleUserId = googleUserId;
  }
  public String getGoogleUserId() {
    return googleUserId;
  }

  public void setGoogleEmail(String googleEmail) {
    if (googleEmail != null) {
      googleEmail = googleEmail.toLowerCase();
    }
    this.googleEmail = googleEmail;
  }
  public String getGoogleEmail() {
    return googleEmail;
  }

  public void setGoogleNickname(String googleNickname) {
    this.googleNickname = googleNickname;
  }
  public String getGoogleNickname() {
    return googleNickname;
  }

  public void setLoginUrl() {
    UserService userService = UserServiceFactory.getUserService();
    loginUrl = userService.createLoginURL("/");
  }
  public String getLoginUrl() {
    return loginUrl;
  }

  public void setLogoutUrl() {
    UserService userService = UserServiceFactory.getUserService();
    logoutUrl = userService.createLogoutURL("/");
  }
  public String getLogoutUrl() {
    return logoutUrl;
  }


  public static UserData createUserData() {
    UserData userData = null; 

    UserService userService = UserServiceFactory.getUserService();
    if (userService.isUserLoggedIn() == false) {
      UserData r = new UserData();
      r.setLoginUrl();
      //r.setLogoutUrl(); // not needed
      return r;
    }

    User u = userService.getCurrentUser();
    if (u == null) {
      UserData r = new UserData();
      r.setLoginUrl();
      //r.setLogoutUrl(); // not needed
      return r;
    }

    // has the user been here before? lookup and or create
    userData = findUserDataByGoogleUserId(u.getUserId());
    if (userData == null) {
      userData = new UserData();
    }

    userData.setGoogleUserId(u.getUserId());
    userData.setGoogleEmail(u.getEmail());
    userData.setGoogleNickname(u.getNickname());
    //userData.setLoginUrl(); // not needed
    userData.setLogoutUrl();

    return userData.persist();
  }

  public UserData persist() {
    
    // JPA does this automatically, but JDO won't. Not sure why.
    version++;
    
    PersistenceManager pm = getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      tx.begin();
      pm.makePersistent(this);
      tx.commit();
    } finally {
      pm.close();
    }
    return this;
  }

  public void remove() {
    PersistenceManager pm = getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      UserData e = pm.getObjectById(UserData.class, key);
      tx.begin();
      pm.deletePersistent(e);
      tx.commit();
    } finally {
      pm.close();
    }
  }
}
