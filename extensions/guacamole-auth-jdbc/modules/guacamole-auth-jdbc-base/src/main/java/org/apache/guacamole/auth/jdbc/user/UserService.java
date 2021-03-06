/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.jdbc.user;

import com.auteve.guacamole.auth.jdbc.yubikey.YubikeyValidator;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectMapper;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectService;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionModel;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionMapper;
import org.apache.guacamole.auth.jdbc.security.PasswordEncryptionService;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.PasswordField;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating users.
 *
 * @author Michael Jumper, James Muehlner
 */
public class UserService extends ModeledDirectoryObjectService<ModeledUser, User, UserModel> {
    
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * All user permissions which are implicitly granted to the new user upon
     * creation.
     */
    private static final ObjectPermission.Type[] IMPLICIT_USER_PERMISSIONS = {
        ObjectPermission.Type.READ
    };

    /**
     * The name of the HTTP password parameter to expect if the user is
     * changing their expired password upon login.
     */
    private static final String NEW_PASSWORD_PARAMETER = "new-password";

    /**
     * The password field to provide the user when their password is expired
     * and must be changed.
     */
    private static final Field NEW_PASSWORD = new PasswordField(NEW_PASSWORD_PARAMETER);

    /**
     * The name of the HTTP password confirmation parameter to expect if the
     * user is changing their expired password upon login.
     */
    private static final String CONFIRM_NEW_PASSWORD_PARAMETER = "confirm-new-password";

    /**
     * The password confirmation field to provide the user when their password
     * is expired and must be changed.
     */
    private static final Field CONFIRM_NEW_PASSWORD = new PasswordField(CONFIRM_NEW_PASSWORD_PARAMETER);

    /**
     * Information describing the expected credentials if a user's password is
     * expired. If a user's password is expired, it must be changed during the
     * login process.
     */
    private static final CredentialsInfo EXPIRED_PASSWORD = new CredentialsInfo(Arrays.asList(
        CredentialsInfo.USERNAME,
        CredentialsInfo.PASSWORD,
        NEW_PASSWORD,
        CONFIRM_NEW_PASSWORD
    ));

    /**
     * Mapper for accessing users.
     */
    @Inject
    private UserMapper userMapper;

    /**
     * Mapper for manipulating user permissions.
     */
    @Inject
    private UserPermissionMapper userPermissionMapper;
    
    /**
     * Provider for creating users.
     */
    @Inject
    private Provider<ModeledUser> userProvider;

    /**
     * Service for hashing passwords.
     */
    @Inject
    private PasswordEncryptionService encryptionService;

    @Override
    protected ModeledDirectoryObjectMapper<UserModel> getObjectMapper() {
        return userMapper;
    }

    @Override
    protected ObjectPermissionMapper getPermissionMapper() {
        return userPermissionMapper;
    }

    @Override
    protected ModeledUser getObjectInstance(AuthenticatedUser currentUser,
            UserModel model) {
        ModeledUser user = userProvider.get();
        user.init(currentUser, model);
        return user;
    }

    @Override
    protected UserModel getModelInstance(AuthenticatedUser currentUser,
            final User object) {

        // Create new ModeledUser backed by blank model
        UserModel model = new UserModel();
        ModeledUser user = getObjectInstance(currentUser, model);

        // Set model contents through ModeledUser, copying the provided user
        user.setIdentifier(object.getIdentifier());
        user.setPassword(object.getPassword());
        user.setYubikey(object.getYubikey());
        user.setAttributes(object.getAttributes());

        return model;
        
    }

    @Override
    protected boolean hasCreatePermission(AuthenticatedUser user)
            throws GuacamoleException {

        // Return whether user has explicit user creation permission
        SystemPermissionSet permissionSet = user.getUser().getSystemPermissions();
        return permissionSet.hasPermission(SystemPermission.Type.CREATE_USER);

    }

    @Override
    protected ObjectPermissionSet getPermissionSet(AuthenticatedUser user)
            throws GuacamoleException {

        // Return permissions related to users
        return user.getUser().getUserPermissions();

    }

    @Override
    protected void beforeCreate(AuthenticatedUser user, UserModel model)
            throws GuacamoleException {

        super.beforeCreate(user, model);
        
        // Username must not be blank
        if (model.getIdentifier() == null || model.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("The username must not be blank.");
        
        // Do not create duplicate users
        Collection<UserModel> existing = userMapper.select(Collections.singleton(model.getIdentifier()));
        if (!existing.isEmpty())
            throw new GuacamoleClientException("User \"" + model.getIdentifier() + "\" already exists.");

    }

    @Override
    protected void beforeUpdate(AuthenticatedUser user,
            UserModel model) throws GuacamoleException {

        super.beforeUpdate(user, model);
        
        // Username must not be blank
        if (model.getIdentifier() == null || model.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("The username must not be blank.");
        
        // Check whether such a user is already present
        UserModel existing = userMapper.selectOne(model.getIdentifier());
        if (existing != null) {

            // Do not rename to existing user
            if (!existing.getObjectID().equals(model.getObjectID()))
                throw new GuacamoleClientException("User \"" + model.getIdentifier() + "\" already exists.");
            
        }
        
    }

    @Override
    protected Collection<ObjectPermissionModel>
        getImplicitPermissions(AuthenticatedUser user, UserModel model) {
            
        // Get original set of implicit permissions
        Collection<ObjectPermissionModel> implicitPermissions = super.getImplicitPermissions(user, model);
        
        // Grant implicit permissions to the new user
        for (ObjectPermission.Type permissionType : IMPLICIT_USER_PERMISSIONS) {
            
            ObjectPermissionModel permissionModel = new ObjectPermissionModel();
            permissionModel.setUserID(model.getObjectID());
            permissionModel.setUsername(model.getIdentifier());
            permissionModel.setType(permissionType);
            permissionModel.setObjectIdentifier(model.getIdentifier());

            // Add new permission to implicit permission set 
            implicitPermissions.add(permissionModel);
            
        }
        
        return implicitPermissions;
    }
        
    @Override
    protected void beforeDelete(AuthenticatedUser user, String identifier) throws GuacamoleException {

        super.beforeDelete(user, identifier);

        // Do not allow users to delete themselves
        if (identifier.equals(user.getUser().getIdentifier()))
            throw new GuacamoleUnsupportedException("Deleting your own user is not allowed.");

    }
    
    int counter = 0;
    
    /**
     * Retrieves the user corresponding to the given credentials from the
     * database. If the user account is expired, and the credentials contain
     * the necessary additional parameters to reset the user's password, the
     * password is reset.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider on behalf of which the user is being
     *     retrieved.
     *
     * @param credentials
     *     The credentials to use when locating the user.
     *
     * @return
     *     An AuthenticatedUser containing the existing ModeledUser object if
     *     the credentials given are valid, null otherwise.
     *
     * @throws GuacamoleException
     *     If the provided credentials to not conform to expectations.
     */
    public AuthenticatedUser retrieveAuthenticatedUser(AuthenticationProvider authenticationProvider,
            Credentials credentials) throws GuacamoleException {
    	
        // Get username and password
        String username = credentials.getUsername();
        String password = credentials.getPassword();
        String yubikey = credentials.getYubikey();
       
        // Retrieve corresponding user model, if such a user exists
        UserModel userModel = userMapper.selectOne(username);
        if (userModel == null)
            return null;

        // If user is disabled, pretend user does not exist
        if (userModel.isDisabled())
            return null;
        
        // Verify provided password is correct
        byte[] hash = encryptionService.createPasswordHash(password, userModel.getPasswordSalt());
        if (!Arrays.equals(hash, userModel.getPasswordHash()))
            return null;
        
        if(!userModel.isExpired()){
        	//Validate yubikey if password has not been expired
        	if(yubikey == null || yubikey.isEmpty() || !YubikeyValidator.validate(yubikey, userModel.getYubikey()))
        		return null;
        }
  
        // Create corresponding user object, set up cyclic reference
        ModeledUser user = getObjectInstance(null, userModel);
        user.setCurrentUser(new AuthenticatedUser(authenticationProvider, user, credentials));

        // Verify user account is still valid as of today
        if (!user.isAccountValid())
            throw new GuacamoleClientException("LOGIN.ERROR_NOT_VALID");

        // Verify user account is allowed to be used at the current time
        if (!user.isAccountAccessible())
            throw new GuacamoleClientException("LOGIN.ERROR_NOT_ACCESSIBLE");
        
        // Update password if password is expired
        if(userModel.isExpired()){
            // Pull new password from HTTP request
            HttpServletRequest request = credentials.getRequest();
            String newPassword = request.getParameter(NEW_PASSWORD_PARAMETER);
            String confirmNewPassword = request.getParameter(CONFIRM_NEW_PASSWORD_PARAMETER);

            // Require new password if account is expired
            if (newPassword == null || confirmNewPassword == null) {
                logger.info("The password of user \"{}\" has expired and must be reset.", username);
                throw new GuacamoleInsufficientCredentialsException("LOGIN.INFO_PASSWORD_EXPIRED", EXPIRED_PASSWORD);
            }

            // New password must be different from old password
            if (newPassword.equals(credentials.getPassword()))
                throw new GuacamoleClientException("LOGIN.ERROR_PASSWORD_SAME");

            // New password must not be blank
            if (newPassword.isEmpty())
                throw new GuacamoleClientException("LOGIN.ERROR_PASSWORD_BLANK");

            // Confirm that the password was entered correctly twice
            if (!newPassword.equals(confirmNewPassword))
                throw new GuacamoleClientException("LOGIN.ERROR_PASSWORD_MISMATCH");

            // Change password and reset expiration flag
            userModel.setExpired(false);
            user.setPassword(newPassword);
            userMapper.update(userModel);
            logger.info("Expired password of user \"{}\" has been reset.", username);
            
        }
   
        // Return now-authenticated user
        return user.getCurrentUser();

    }

    /**
     * Retrieves the user corresponding to the given AuthenticatedUser from the
     * database.
     *
     * @param authenticatedUser
     *     The AuthenticatedUser to retrieve the corresponding ModeledUser of.
     *
     * @return
     *     The ModeledUser which corresponds to the given AuthenticatedUser, or
     *     null if no such user exists.
     */
    public ModeledUser retrieveUser(org.apache.guacamole.net.auth.AuthenticatedUser authenticatedUser) {

        // If we already queried this user, return that rather than querying again
        if (authenticatedUser instanceof AuthenticatedUser)
            return ((AuthenticatedUser) authenticatedUser).getUser();

        // Get username
        String username = authenticatedUser.getIdentifier();

        // Retrieve corresponding user model, if such a user exists
        UserModel userModel = userMapper.selectOne(username);
        if (userModel == null)
            return null;

        // Create corresponding user object, set up cyclic reference
        ModeledUser user = getObjectInstance(null, userModel);
        user.setCurrentUser(new AuthenticatedUser(authenticatedUser.getAuthenticationProvider(), user, authenticatedUser.getCredentials()));

        // Return already-authenticated user
        return user;

    }

}
