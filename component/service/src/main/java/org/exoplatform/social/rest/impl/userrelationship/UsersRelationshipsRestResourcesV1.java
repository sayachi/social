/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.social.rest.impl.userrelationship;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.rest.api.UsersRelationshipsRestResources;
import org.exoplatform.social.rest.entity.CollectionEntity;
import org.exoplatform.social.rest.entity.DataEntity;
import org.exoplatform.social.rest.entity.RelationshipEntity;
import org.exoplatform.social.service.rest.api.VersionResources;

@Path(VersionResources.VERSION_ONE + "/social/usersRelationships")
@Api(value=VersionResources.VERSION_ONE + "/social/usersRelationships", description = "Operations eXo Platform user relationships.")
public class UsersRelationshipsRestResourcesV1 implements UsersRelationshipsRestResources {

  public UsersRelationshipsRestResourcesV1() {
  }
  
  @RolesAllowed("users")
  @GET
  @ApiOperation(value = "Get relationships",
                httpMethod = "GET",
                response = Response.class,
                notes = "This can only be done by the logged in user.")
  @ApiResponses(value = { 
    @ApiResponse (code = 200, message = "Given request relationships found"),
    @ApiResponse (code = 500, message = "Internal server error"),
    @ApiResponse (code = 400, message = "Invalid query input to find relationships.") })
  public Response getUsersRelationships(@Context UriInfo uriInfo,
                                        @ApiParam(value = "Status of relationship: pending, cofirmed, ignored, all") @QueryParam("status") String status,
                                        @ApiParam(value = "User to get relationship (remoteId such as root, demo..)") @QueryParam("user") String user,
                                        @ApiParam(value = "Offset", required = false, defaultValue = "0") @QueryParam("offset") int offset,
                                        @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit,
                                        @ApiParam(value = "Size of returned result list.", defaultValue = "false") @QueryParam("returnSize") boolean returnSize,
                                        @ApiParam(value = "Expand param : ask for a full representation of a subresource", required = false) @QueryParam("expand") String expand) throws Exception {
    
    offset = offset > 0 ? offset : RestUtils.getOffset(uriInfo);
    limit = limit > 0 ? limit : RestUtils.getLimit(uriInfo);
    
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    Relationship.Type type;
    
    try {
      type = Relationship.Type.valueOf(status.toUpperCase());
    } catch (Exception e) {
      type = Relationship.Type.ALL;
    }
    
    List<Relationship> relationships = new ArrayList<Relationship>();
    int size = 0;
    
    if (user != null & RestUtils.isMemberOfAdminGroup()) {
      Identity givenUser = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, user, true);
      relationships = relationshipManager.getRelationshipsByStatus(givenUser, type, offset, limit);
      size = returnSize ? relationshipManager.getRelationshipsCountByStatus(givenUser, type) : -1;
    } else {
      Identity authenticatedUser = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, ConversationState.getCurrent().getIdentity().getUserId(), true);
      relationships = relationshipManager.getRelationshipsByStatus(authenticatedUser, type, offset, limit);
      size = returnSize ? relationshipManager.getRelationshipsCountByStatus(authenticatedUser, type) : -1;
    }
    
    List<DataEntity> relationshipEntities = EntityBuilder.buildRelationshipEntities(relationships, uriInfo);
    CollectionEntity collectionRelationship = new CollectionEntity(relationshipEntities, EntityBuilder.USERS_RELATIONSHIP_TYPE, offset, limit);
    if (returnSize) {
      collectionRelationship.setSize(size);
    }    
    return EntityBuilder.getResponse(collectionRelationship, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @POST
  @RolesAllowed("users")
  @ApiOperation(value = "Create relationship of user",
                httpMethod = "POST",
                response = Response.class,
                notes = "This can only be done by the logged in user.")
  @ApiResponses(value = { 
    @ApiResponse (code = 200, message = "Relationship of user created successfully"),
    @ApiResponse (code = 500, message = "Internal server error"),
    @ApiResponse (code = 400, message = "Invalid query input to create user relationship.") })
  public Response createUsersRelationships(@Context UriInfo uriInfo,
                                           @ApiParam(value = "Expand param : ask for a full representation of a subresource", required = false) @QueryParam("expand") String expand,
                                           @ApiParam(value = "Created relationship object. Sender(username), Receiver(username) and Status(pending, confirmed, ignored)) are required.", required = true) RelationshipEntity model) throws Exception {
    if (model == null || model.getReceiver() == null || model.getSender() == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    if (! RestUtils.isMemberOfAdminGroup() && !model.getReceiver().equals(authenticatedUser) && !model.getSender().equals(authenticatedUser)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity sender = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, model.getSender(), true);
    Identity receiver = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, model.getReceiver(), true);
    if (sender == null || receiver == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    Relationship.Type status = model.getStatus() != null && model.getStatus().equalsIgnoreCase("pending") ? Relationship.Type.PENDING : Relationship.Type.CONFIRMED;
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    if (relationshipManager.get(sender, receiver) != null) {
      throw new WebApplicationException(Response.Status.PRECONDITION_FAILED);
    }
    Relationship relationship = new Relationship(sender, receiver, status);
    relationshipManager.update(relationship);
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityRelationship(relationship, uriInfo.getPath(), expand, false), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @RolesAllowed("users")
  @Path("{id}")
  @ApiOperation(value = "Get relationship of user by Id",
                httpMethod = "GET",
                response = Response.class,
                notes = "This can only be done by the logged in user.")
  @ApiResponses(value = { 
    @ApiResponse (code = 200, message = "Given request user relationships found"),
    @ApiResponse (code = 500, message = "Internal server error"),
    @ApiResponse (code = 400, message = "Invalid query input to find user relationship.") })
  public Response getUsersRelationshipsById(@Context UriInfo uriInfo,
                                            @ApiParam(value = "User remoteId", required = true) @PathParam("id") String id,
                                            @ApiParam(value = "Expand param : ask for a full representation of a subresource", required = false) @QueryParam("expand") String expand) throws Exception {
    
    Identity authenticatedUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, ConversationState.getCurrent().getIdentity().getUserId(), true);
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    Relationship relationship = relationshipManager.get(id);
    if (relationship == null || ! hasPermissionOnRelationship(authenticatedUser, relationship)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    return EntityBuilder.getResponse(EntityBuilder.buildEntityRelationship(relationship, uriInfo.getPath(), expand, false), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @PUT
  @RolesAllowed("users")
  @Path("{id}")
  @ApiOperation(value = "Update relationship by Id",
                httpMethod = "PUT",
                response = Response.class,
                notes = "This can only be done by the logged in user.")
  @ApiResponses(value = { 
    @ApiResponse (code = 200, message = "Given request relationship updated successfully"),
    @ApiResponse (code = 500, message = "Internal server error"),
    @ApiResponse (code = 400, message = "Invalid query input to upudate relationship.") })
  public Response updateUsersRelationshipsById(@Context UriInfo uriInfo,
                                               @ApiParam(value = "User name", required = true) @PathParam("id") String id,
                                               @ApiParam(value = "Expand param : ask for a full representation of a subresource", required = false) @QueryParam("expand") String expand,
                                               @ApiParam(value = "Relationship object for updating", required = true) RelationshipEntity model) throws Exception {
    if (model == null || model.getStatus() == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    Identity authenticatedUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, ConversationState.getCurrent().getIdentity().getUserId(), true);
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    Relationship relationship = relationshipManager.get(id);
    if (relationship == null || ! hasPermissionOnRelationship(authenticatedUser, relationship)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    Relationship.Type status;
    try {
      status = Relationship.Type.valueOf(model.getStatus().toUpperCase());
    } catch (Exception e) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //update relationship by status
    updateRelationshipByStatus(relationship, status, relationshipManager);
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityRelationship(relationship, uriInfo.getPath(), expand, false), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @DELETE
  @RolesAllowed("users")
  @Path("{id}")
  @ApiOperation(value = "Delete a relationship of user by Id",
                httpMethod = "DELETE",
                response = Response.class,
                notes = "This can only be done by the logged in user.")
  @ApiResponses(value = { 
    @ApiResponse (code = 200, message = "Given request relationship deleted successfully"),
    @ApiResponse (code = 500, message = "Internal server error"),
    @ApiResponse (code = 400, message = "Invalid query input to delete relationship.") })
  @ApiImplicitParams({
    @ApiImplicitParam(name = "id", value = "Id of relationship to delete", dataType = "string", paramType = "path"),
    @ApiImplicitParam(name = "expand", value = "Expand param : ask for a full representation of a subresource", dataType = "string", paramType = "query") })
  public Response deleteUsersRelationshipsById(@Context UriInfo uriInfo,
                                               @ApiParam(value = "User name", required = true) @PathParam("id") String id,
                                               @ApiParam(value = "Expand param : ask for a full representation of a subresource", required = false) @QueryParam("expand") String expand) throws Exception {
    
    Identity authenticatedUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, ConversationState.getCurrent().getIdentity().getUserId(), true);
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    Relationship relationship = relationshipManager.get(id);
    if (relationship == null || ! hasPermissionOnRelationship(authenticatedUser, relationship)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //delete the relationship
    relationshipManager.delete(relationship);
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityRelationship(relationship, uriInfo.getPath(), expand, false), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }

  /**
   * Check if the viewer is an administrator or the receiver or the sender of relationship
   * 
   * @param authenticatedUser
   * @param relationship
   * @return
   */
  private boolean hasPermissionOnRelationship(Identity authenticatedUser, Relationship relationship) {
    if (RestUtils.isMemberOfAdminGroup()) return true;
    if (authenticatedUser.getId().equals(relationship.getSender().getId())
        || authenticatedUser.getId().equals(relationship.getReceiver().getId())) {
      return true;
    }
    return false;
  }
  
  private void updateRelationshipByStatus(Relationship relationship, Relationship.Type status, RelationshipManager relationshipManager) {
    switch (status) {
      case IGNORED: {//from confirm or pending to ignore
        relationshipManager.delete(relationship);
        break;
      }
      case PENDING: {//from confirm to pending but this case doesn't exist
        break;
      }
      case CONFIRMED: {//from pending to confirm
        relationship.setStatus(status);
        relationshipManager.update(relationship);
        break;
      }
      default:
        break;
      }
  }
}
