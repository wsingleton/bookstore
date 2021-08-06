package com.revature.bookstore.repos;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.revature.bookstore.documents.AppUser;

import com.revature.bookstore.util.MongoClientFactory;
import com.revature.bookstore.util.PasswordUtils;
import com.revature.bookstore.util.exceptions.DataSourceException;
import org.bson.Document;

public class UserRepository implements CrudRepository<AppUser> {

    public AppUser findUserByCredentials(String username, String password) {

        try {
            MongoClient mongoClient = MongoClientFactory.getInstance().getConnection();
            MongoDatabase bookstoreDatabase = mongoClient.getDatabase("bookstore");
            MongoCollection<Document> usersCollection = bookstoreDatabase.getCollection("users");
            Document queryDoc = new Document("username", username);
            Document authUserDoc = usersCollection.find(queryDoc).first();

            if (authUserDoc == null) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            AppUser authUser = mapper.readValue(authUserDoc.toJson(), AppUser.class);
            authUser.setId(authUserDoc.get("_id").toString());

            // [Contains Legacy Logic] -- Verify password match: return user if correct
            if(authUser.getKey() != null &&
                    PasswordUtils.verifyUserPassword(password, authUser.getPassword(), authUser.getKey())) {
                System.out.println("Pass: " + password + ", Encryp: " + authUser.getPassword() + ", Key: " + authUser.getKey());
                return authUser;
            } else if(authUser.getPassword().equals(password)) {
                System.out.println("Password: " + password);
                return authUser;
            } else {
                System.out.println("Pass: " + password + ", Encryp: " + authUser.getPassword() + ", Key: " + authUser.getKey());
                return null;
            }

        } catch (JsonMappingException jme) {
            jme.printStackTrace(); // TODO log this to a file
            throw new DataSourceException("An exception occurred while mapping the document.", jme);
        } catch (Exception e) {
            e.printStackTrace(); // TODO log this to a file
            throw new DataSourceException("An unexpected exception occurred.", e);
        }

    }

    // TODO implement this so that we can prevent multiple users from having the same username!
    public AppUser findUserByUsername(String username) {
        return null;
    }

    @Override
    public AppUser findById(int id) {
        return null;
    }

    @Override
    public AppUser save(AppUser newUser) {

        try {
            // Encrypt newUser's plaintext password
            String salt = PasswordUtils.getSalt(30);
            String encryptedPassword = PasswordUtils.generateSecurePassword(newUser.getPassword(), salt);

            MongoClient mongoClient = MongoClientFactory.getInstance()
                                                        .getConnection();

            MongoDatabase bookstoreDb = mongoClient.getDatabase("bookstore");
            MongoCollection<Document> usersCollection = bookstoreDb.getCollection("users");
            Document newUserDoc = new Document("firstName", newUser.getFirstName())
                    .append("lastName", newUser.getLastName())
                    .append("email", newUser.getEmail())
                    .append("username", newUser.getUsername())
                    .append("password", encryptedPassword)
                    .append("key", salt);

            usersCollection.insertOne(newUserDoc);
            newUser.setId(newUserDoc.get("_id").toString());

            return newUser;

        } catch (Exception e) {
            e.printStackTrace(); // TODO log this to a file
            throw new DataSourceException("An unexpected exception occurred.", e);
        }

    }

    @Override
    public boolean update(AppUser updatedResource) {
        return false;
    }

    @Override
    public boolean deleteById(int id) {
        return false;
    }

}
