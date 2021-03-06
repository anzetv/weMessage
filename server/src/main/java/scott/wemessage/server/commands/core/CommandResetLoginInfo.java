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

package scott.wemessage.server.commands.core;

import java.util.Scanner;

import scott.wemessage.commons.crypto.BCrypt;
import scott.wemessage.commons.types.DisconnectReason;
import scott.wemessage.commons.utils.AuthenticationUtils;
import scott.wemessage.server.ServerLogger;
import scott.wemessage.server.commands.CommandManager;
import scott.wemessage.server.configuration.ServerConfiguration;
import scott.wemessage.server.configuration.json.ConfigJSON;
import scott.wemessage.server.connection.Device;
import scott.wemessage.server.connection.DeviceManager;
import scott.wemessage.server.security.Authenticator;
import scott.wemessage.server.weMessage;

public class CommandResetLoginInfo extends CoreCommand {

    public CommandResetLoginInfo(CommandManager manager){
        super(manager, "resetlogininfo", "Resets the login information needed for weMessage Clients to connect to your weServer.", new String[]{ "newpassword", "resetpassword", "resetlogin", "resetpass", "resetaccountinfo", "newlogin" });
    }

    public void execute(String[] args){
        ServerLogger.log("In order to change your account login information, please enter the password you are currently using.");
        ServerLogger.emptyLine();
        ServerConfiguration configuration = getCommandManager().getMessageServer().getConfiguration();
        ConfigJSON configJSON;
        boolean isEmailNotAuthenticated = true;
        boolean isPasswordNotAuthenticated = true;
        boolean hasNotProvidedPastPassword = true;
        boolean pastPasscodeWrong = false;

        try {
            configJSON = configuration.getConfigJSON();
        }catch (Exception ex){
            ServerLogger.error("Could not get previous account login info.", ex);
            return;
        }

        Scanner lastPassScanner = new Scanner(System.in);

        while(hasNotProvidedPastPassword){
            String pastPassword = lastPassScanner.nextLine();
            if (!BCrypt.checkPassword(pastPassword, configJSON.getConfig().getAccountInfo().getPassword())){
                ServerLogger.log("The password entered does not match the current password. Exiting configuration.");
                hasNotProvidedPastPassword = false;
                pastPasscodeWrong = true;
            }else {
                hasNotProvidedPastPassword = false;
            }
        }

        if(pastPasscodeWrong) return;

        DeviceManager deviceManager = getCommandManager().getMessageServer().getDeviceManager();

        ServerLogger.emptyLine();

        if (deviceManager.getDevices().size() > 0) {
            ServerLogger.log("Entering Edit Config Mode. Disconnecting all connected devices...");

            for (Device device : getCommandManager().getMessageServer().getDeviceManager().getDevices().values()) {
                deviceManager.removeDevice(device, DisconnectReason.FORCED, null);
            }
        }else {
            ServerLogger.log("Entering Edit Config Mode.");
            ServerLogger.emptyLine();
        }

        Scanner emailScanner = new Scanner(System.in);

        ServerLogger.log("Please enter a new email for devices to connect with!");
        ServerLogger.log("Your email must be the same as the one you are using iMessage with.");
        ServerLogger.emptyLine();

        while (isEmailNotAuthenticated){
            String email = emailScanner.nextLine();

            if (!Authenticator.isValidEmailFormat(email) || email.equalsIgnoreCase(weMessage.DEFAULT_EMAIL)) {
                ServerLogger.log("The email you provided is not a valid address.");
                ServerLogger.emptyLine();
            } else {
                try {
                    configJSON.getConfig().getAccountInfo().setEmail(email);
                    isEmailNotAuthenticated = false;
                } catch (Exception ex) {
                    ServerLogger.error("An error occurred while trying to set a login email address. Shutting down!", ex);
                    getCommandManager().getMessageServer().shutdown(-1, false);
                    return;
                }
            }
        }

        ServerLogger.emptyLine();
        ServerLogger.log("Please enter a new password for devices to connect with!");
        ServerLogger.emptyLine();

        Scanner passwordScanner = new Scanner(System.in);

        while (isPasswordNotAuthenticated) {
            String password = passwordScanner.nextLine();
            AuthenticationUtils.PasswordValidateType validateType = AuthenticationUtils.isValidPasswordFormat(password);

            if (validateType == AuthenticationUtils.PasswordValidateType.LENGTH_TOO_SMALL){
                ServerLogger.log("The password you have provided is too short.");
                ServerLogger.log("Please provide a password that is at least " + weMessage.MINIMUM_PASSWORD_LENGTH + " characters in length.");
                ServerLogger.emptyLine();
                continue;
            }

            if (validateType == AuthenticationUtils.PasswordValidateType.PASSWORD_TOO_EASY){
                ServerLogger.log("The password you have provided is too easy to guess.");
                ServerLogger.log("Please provide a more complex password.");
                ServerLogger.emptyLine();
                continue;
            }

            if (validateType == AuthenticationUtils.PasswordValidateType.VALID) {
                try {
                    String secret = BCrypt.generateSalt();
                    String passwordHash = BCrypt.hashPassword(password, secret);

                    configJSON.getConfig().getAccountInfo().setSecret(secret);
                    configJSON.getConfig().getAccountInfo().setPassword(passwordHash);

                    configuration.writeJsonToConfig(configJSON);
                    isPasswordNotAuthenticated = false;
                    ServerLogger.emptyLine();
                    ServerLogger.log("Login information successfully updated.");
                } catch (Exception ex) {
                    ServerLogger.error("An error occurred while trying to set a password. Shutting down!", ex);
                    getCommandManager().getMessageServer().shutdown(-1, false);
                }
            }
        }
    }
}