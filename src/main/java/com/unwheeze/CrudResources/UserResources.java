package com.unwheeze.CrudResources;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rethinkdb.gen.ast.Json;
import com.unwheeze.beans.User;
import com.unwheeze.database.DbScheme;
import com.unwheeze.database.UnwheezeDb;
import com.unwheeze.security.FieldCrypter;
import com.unwheeze.security.JWTBuilder;
import com.unwheeze.utils.ByteConverter;
import com.unwheeze.utils.JsonErrorStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;


@Path("users/")
public class UserResources {

    private final static Logger log = LogManager.getLogger(UserResources.class.getName());

    Gson gson = new Gson();
    private final String REGEX_EMAIL="^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    @POST
    @Path("/signup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertNewUser(String json) {

        UnwheezeDb db = new UnwheezeDb();
        JsonObject userJson = gson.fromJson(json,JsonObject.class);

        String email = userJson.get(DbScheme.USERS_EMAIL).getAsString();
        boolean userInDb = db.isUserInCollection(email, DbScheme.USERS_EMAIL);
        if(!userInDb)
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(JsonErrorStatus.errorEmailInDb)
                    .build();

        String pwd = "";
        try {
            pwd = new String(Base64.decode(userJson.get(DbScheme.USERS_PWD).getAsString()),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if(pwd.isEmpty() || pwd == null){
            String errorCodeEmptyPwd = "{'errorStatus':'Password field is empty'}";
            return Response.status(Response.Status.EXPECTATION_FAILED)
                    .entity(errorCodeEmptyPwd)
                    .build();
        }
        //-----Compute pwd

        byte[] salt = FieldCrypter.generateRandomSalt();
        byte[] encryptedPwd = FieldCrypter.generateHash(pwd.toCharArray(),salt);
        userJson.addProperty(DbScheme.USERS_SALT, ByteConverter.HexToStr(salt));
        userJson.addProperty(DbScheme.USERS_PWD,ByteConverter.HexToStr(encryptedPwd));
        //-----------------
        String uuid = db.generateUUID();
        userJson.addProperty(DbScheme.USERS_ID,uuid);
        User user = gson.fromJson(userJson,User.class);

        //------ Verifying email
        boolean matchingEmail = Pattern.compile(this.REGEX_EMAIL)
                .matcher(email)
                .matches();
        if(!matchingEmail)
            return Response.status(Response.Status.EXPECTATION_FAILED)
            .entity(JsonErrorStatus.errorInvalidFormatEmail)
            .build();
        
        //TODO : Send confirmation email
        int responseInsert = db.putUserInCollection(user);

        switch(responseInsert) {
            case -1:
                log.error(JsonErrorStatus.errorCodeInsertion);
                return Response.status(Response.Status.EXPECTATION_FAILED)
                        .entity(JsonErrorStatus.errorCodeInsertion)
                        .build();
            case -2:
                log.error(JsonErrorStatus.errorDbNotInit);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(JsonErrorStatus.errorDbNotInit)
                        .build();
            default:
                log.debug("Insertion done with no errors");
                String userDoc = "";
                try {
                    userDoc = db.getUserFromCollection(email, DbScheme.USERS_EMAIL);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                return Response.status(Response.Status.OK)
                        .entity(userDoc)
                        .build();
        }

    }

    @POST
    @Path("/signin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response signInUser(@Context HttpHeaders header) {
        log.info("Signing in user");

        String authHeader = header.getHeaderString("Authorization");
        String jwt;
        try {
            jwt = new String(Base64.decode(authHeader.split("Bearer")[1]),"UTF-8");
        } catch(UnsupportedEncodingException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        Claims claims;
        try {
            claims = (new JWTBuilder()).getClaims(jwt);
        } catch(ExpiredJwtException | UnsupportedJwtException | MalformedJwtException
                | IllegalArgumentException | IOException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if(claims.isEmpty())
            return Response.status(Response.Status.EXPECTATION_FAILED)
                    .entity(JsonErrorStatus.errorInvalidAuthTkn)
                    .build();

        String userId = claims.getSubject().split("user/")[1];
        UnwheezeDb db = new UnwheezeDb();

        boolean userExists = db.isUserInCollection(userId,DbScheme.USERS_ID);

        if(!userExists)
            return Response.status(Response.Status.BAD_REQUEST).entity(JsonErrorStatus.errorEmailNotInDb).build();

        String userDoc;
        try {
            userDoc = db.getUserFromCollection(userId,DbScheme.USERS_ID);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        JsonObject jsonUser = gson.fromJson(userDoc, JsonObject.class);
        jsonUser.remove(DbScheme.USERS_PWD);
        jsonUser.remove(DbScheme.USERS_SALT);

        userDoc = gson.toJson(jsonUser);

        return Response.status(Response.Status.OK)
                .entity(userDoc).build();
    }


}
