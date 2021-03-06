/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.social.core.storage.cache;

import org.exoplatform.social.core.identity.model.ActiveIdentityFilter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.test.AbstractCoreTest;
import org.exoplatform.social.core.test.MaxQueryNumber;
import org.exoplatform.social.core.test.QueryNumberTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
@QueryNumberTest
public class JCRCachedIdentityStorageTestCase extends AbstractCoreTest {

  private CachedIdentityStorage cachedIdentityStorage;
  private SocialStorageCacheService cacheService;

  private List<String> tearDownIdentityList;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    cachedIdentityStorage = getContainer().getComponentInstanceOfType(JCRCachedIdentityStorage.class);

    cacheService = getContainer().getComponentInstanceOfType(SocialStorageCacheService.class);
    cacheService.getIdentityCache().clearCache();
    cacheService.getIdentitiesCache().clearCache();
    cacheService.getCountIdentitiesCache().clearCache();
    cacheService.getIdentityIndexCache().clearCache();
    cacheService.getProfileCache().clearCache();

    tearDownIdentityList = new ArrayList<String>();
  }

  @Override
  public void tearDown() throws Exception {
    for (String id : tearDownIdentityList) {
      cachedIdentityStorage.deleteIdentity(new Identity(id));
    }
    super.tearDown();
  }

  @MaxQueryNumber(69)
  public void testSaveIdentity() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getIdentitiesCache().getCacheSize());
    assertEquals(0, cacheService.getCountIdentitiesCache().getCacheSize());

  }

  @MaxQueryNumber(72)
  public void testFindIdentityById() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    String id = i.getId();
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());

    cacheService.getIdentityCache().clearCache();
    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    cachedIdentityStorage.findIdentityById(id);
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());

  }

  @MaxQueryNumber(72)
  public void testFindIdentity() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getIdentityIndexCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getIdentityIndexCache().getCacheSize());

    cacheService.getIdentityCache().clearCache();
    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getIdentityIndexCache().getCacheSize());
    cachedIdentityStorage.findIdentity(OrganizationIdentityProvider.NAME, "id");
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(1, cacheService.getIdentityIndexCache().getCacheSize());

  }

  @MaxQueryNumber(372)
  public void testRemoveIdentity() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getIdentitiesCache().getCacheSize());

    ProfileFilter filter = new ProfileFilter();
    cachedIdentityStorage.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME, filter, 0, 10, false);
    assertEquals(1, cacheService.getIdentitiesCache().getCacheSize());

    cachedIdentityStorage.deleteIdentity(i);
    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getIdentitiesCache().getCacheSize());

  }

  @MaxQueryNumber(81)
  public void testUpdateIdentity() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());

    cachedIdentityStorage.updateIdentity(i);
    assertEquals(0, cacheService.getIdentityCache().getCacheSize());

  }

  @MaxQueryNumber(90)
  public void testLoadProfile() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getProfileCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getProfileCache().getCacheSize());

    cachedIdentityStorage.loadProfile(new Profile(i));
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(1, cacheService.getProfileCache().getCacheSize());

  }

  @MaxQueryNumber(90)
  public void testSaveProfile() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getProfileCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getProfileCache().getCacheSize());

    cachedIdentityStorage.saveProfile(new Profile(i));
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getProfileCache().getCacheSize());

  }

  @MaxQueryNumber(90)
  public void testUpdateProfile() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getProfileCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getProfileCache().getCacheSize());

    cachedIdentityStorage.updateProfile(new Profile(i));
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());
    assertEquals(0, cacheService.getProfileCache().getCacheSize());

  }

  @MaxQueryNumber(258)
  public void testGetIdentitiesByFilterCount() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());

    assertEquals(0, cacheService.getCountIdentitiesCache().getCacheSize());
    cachedIdentityStorage.getIdentitiesByProfileFilterCount(OrganizationIdentityProvider.NAME, new ProfileFilter());
    assertEquals(1, cacheService.getCountIdentitiesCache().getCacheSize());

    Identity i2 = new Identity(OrganizationIdentityProvider.NAME, "id2");
    cachedIdentityStorage.saveIdentity(i2);
    tearDownIdentityList.add(i2.getId());

    assertEquals(0, cacheService.getCountIdentitiesCache().getCacheSize());
    cachedIdentityStorage.getIdentitiesByProfileFilterCount(OrganizationIdentityProvider.NAME, new ProfileFilter());
    assertEquals(1, cacheService.getCountIdentitiesCache().getCacheSize());

    i2.setRemoteId("id3");
    cachedIdentityStorage.updateIdentity(i2);

    assertEquals(0, cacheService.getCountIdentitiesCache().getCacheSize());

  }

  @MaxQueryNumber(138)
  public void testGetIdentitiesByFilter() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());

    assertEquals(0, cacheService.getIdentitiesCache().getCacheSize());
    cachedIdentityStorage.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME, new ProfileFilter(), 0, 10, false);
    assertEquals(1, cacheService.getIdentitiesCache().getCacheSize());

    Identity i2 = new Identity(OrganizationIdentityProvider.NAME, "id2");
    cachedIdentityStorage.saveIdentity(i2);
    tearDownIdentityList.add(i2.getId());

    assertEquals(0, cacheService.getIdentitiesCache().getCacheSize());

  }

  @MaxQueryNumber(84)
  public void testGetIdentitiesByFirstCharacterOfNameCount() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());

    assertEquals(0, cacheService.getCountIdentitiesCache().getCacheSize());
    cachedIdentityStorage.getIdentitiesByFirstCharacterOfNameCount(OrganizationIdentityProvider.NAME, new ProfileFilter());
    assertEquals(1, cacheService.getCountIdentitiesCache().getCacheSize());

  }

  @MaxQueryNumber(84)
  public void testGetIdentitiesByFirstCharacterOfName() throws Exception {

    assertEquals(0, cacheService.getIdentityCache().getCacheSize());
    Identity i = new Identity(OrganizationIdentityProvider.NAME, "id");
    cachedIdentityStorage.saveIdentity(i);
    tearDownIdentityList.add(i.getId());
    assertEquals(1, cacheService.getIdentityCache().getCacheSize());

    assertEquals(0, cacheService.getIdentitiesCache().getCacheSize());
    cachedIdentityStorage.getIdentitiesByFirstCharacterOfName(OrganizationIdentityProvider.NAME, new ProfileFilter(), 0, 10, false);
    assertEquals(1, cacheService.getIdentitiesCache().getCacheSize());

  }
  
  public void testGetActiveUsers() throws Exception {
    ActiveIdentityFilter filter = new ActiveIdentityFilter("/platform/administrators");
    Set<String> activeUsers = cachedIdentityStorage.getActiveUsers(filter);
    //expected root and john are active user.
    assertEquals(2, activeUsers.size());
  }
}
