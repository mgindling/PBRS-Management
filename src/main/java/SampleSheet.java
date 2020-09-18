import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class SampleSheet {

    // Google Spreadsheet interface constants.
    private static final String APPLICATION_NAME = "PBRS Console: Google Sheets API (Java)";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    // NOTE: IF SCOPES IS MODIFIED, THE "tokens/" FOLDER MUST BE DELETED.
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    // Promotion information constants.
    private static final String[] PROMOTION_NAMES =
            {"Commoner (A)", "Initiate (B)", "High Initiate (C)", "Aspirant (D)", "First Aspirant (E)",
             "Guerrilla (F)", "Lance Corporal (G)", "Lance Sergeant (H)", "Marksciath (I)",
             "Markspice (J)", "Cavalier (K)", "Cavalier Major (L)"};
    private static final Integer[] PROMOTION_SOC_VALUES = {0, 1, 3, 6, 10, 14, 22, 29, 36, 44, 55, 75};
    private static final Integer[] PROMOTION_SKC_VALUES = {0, 0, 0, 1, 2, 4, 6, 9, 12, 16, 20, 35};
    private static final Integer[] PROMOTION_CC_VALUES = {0, 0, 0, 1, 2, 3, 5, 6, 8, 10, 12, 20};
    private static final String[] PROMOTION_LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};


    /**
     * Creates an authorized Credential object. Got this from Google; have not modified it.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {

        // Load client secrets.
        InputStream in = SampleSheet.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

    }

    /*
     * Finds the user index of the user specified by username.
     * @param userList -- the complete list of users to search.
     * @param username -- the username that corresponds to the user we want to find.
     * @return -- the index of the user that corresponds to username; -1 if user not found.
     */
    private static Integer findUser(List<List<Object>> userList, String username) {
        for (int v = 0; v < userList.size(); v++) {
            if (userList.get(v).get(2).toString().toUpperCase().equals(username.toUpperCase())) {
                return v;
            }
        }
        // Makes sure the application prints an error if the user was not found.
        return -1;
    }

    /*
     * Runs through the whole list of users and check for promotions for all of them.
     * @param userList -- the complete list of users to check.
     */
    private static void checkAllPromotions(List<List<Object>> userList) {

        // Variables needed for the loop to work.
        int curr_soc, curr_skc, curr_cc;
        String username;

        for (int user_index = 0; user_index < userList.size(); user_index++) {

            // Pulls the current points for the selected user.
            curr_soc = Integer.parseInt(userList.get(user_index).get(4).toString());
            curr_skc = Integer.parseInt(userList.get(user_index).get(5).toString());
            curr_cc = Integer.parseInt(userList.get(user_index).get(6).toString());
            username = userList.get(user_index).get(2).toString();

            // Finds the current SoC rank.
            int highest_soc = 0;
            for (int s = 0; s < PROMOTION_SOC_VALUES.length; s++) {
                if (curr_soc < PROMOTION_SOC_VALUES[s]) {
                    highest_soc = s - 1;
                    break;
                }
                else {
                    if (s == PROMOTION_SOC_VALUES.length - 1) {
                        highest_soc = s;
                        break;
                    }
                }
            }

            // Finds the current SkC rank.
            int highest_skc = 0;
            for (int s = 0; s < PROMOTION_SKC_VALUES.length; s++) {
                if (curr_skc < PROMOTION_SKC_VALUES[s]) {
                    highest_skc = s - 1;
                    break;
                }
                else {
                    if (s == PROMOTION_SKC_VALUES.length - 1) {
                        highest_skc = s;
                        break;
                    }
                }
            }

            // Finds the current CC rank.
            int highest_cc = 0;
            for (int s = 0; s < PROMOTION_CC_VALUES.length; s++) {
                if (curr_cc < PROMOTION_CC_VALUES[s]) {
                    highest_cc = s - 1;
                    break;
                }
                else {
                    if (s == PROMOTION_CC_VALUES.length - 1) {
                        highest_cc = s;
                        break;
                    }
                }
            }

            // The lowest of the three numbers is the highest promotion.
            int curr_rank = Math.min(highest_cc, Math.min(highest_skc, highest_soc));
            String curr_rank_code = PROMOTION_LETTERS[curr_rank];

            // Checks to see if that number is different than the current one.
            if (!userList.get(user_index).get(1).toString().equals(curr_rank_code)) {
                System.out.println("User " + username + " should be promoted to " + PROMOTION_NAMES[curr_rank]);
            }
        }

        System.out.println("No more promotions!");
    }

    /**
     * PBRS application. Connects to PBRS spreadsheet and allows for various commands.
     * NOTE: It is currently impossible to add new users.
     * COMMAND LIST:
     *      getpoints <user> -- gets all points for the user specified; not case sensitive
     *      addpoints <user> <SOC> <SKC> <CC> -- adds the designated points to the user specified.
     *      changepoints <user> <SOC> <SKC> <CC> -- changes the points of the user specified.
     *      checkpromo <user> -- checks promotions for specified users (or all users if user = "all")
     *      done -- ends application
     */
    public static void main(String... args) throws IOException, GeneralSecurityException {

        // If this ever connects to an outside application the input will need to change.
        Scanner scan = new Scanner(System.in);

        // This reads from the range.txt file to get the range of data that must be pulled from the spreadsheet.
        File range_file = new File("src/main/resources/range.txt");
        Scanner range_scan = new Scanner(range_file);
        String temp_range = range_scan.next();
        System.out.println(temp_range);

        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = "1cZTuYFX0nJ1laHsOW3Ll5149iKRgBPx3nf6exllwzuI";

        // NOTE: This accesses the entire PBRS table AS OF NOW. Will need to be modified if rows are added.
        final String range = "B23:H92";

        // Builds application connected to the Google Sheets specified.
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Gets response from Google Sheets based on the given range.
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> values = response.getValues();

        // If the response is somehow empty, the application will end.
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
            return;
        }

        // Legacy Stuff. Prints out a table of every person in the PBRS.
        // NOTE: Default table is set up as Rank Name | Rank Code | Name | User Code | SoC | SkC | CC
        /*
         * else {
         *     System.out.println(" Name | Rank | SoC | SkC | CC ");
         *     System.out.println(" ---------------------------- ");
         *     for (List row : values) {
         *         System.out.printf("%s, %s\n", row.get(2), row.get(0), row.get(4), row.get(5), row.get(6));
         *     }
         * }
         *
         */

        // Not sure if this is needed or not.
        // boolean updateValues = false;

        // The console runs forever. Input is not taken line-by-line but rather in chunks of words.
        while (true) {

            /* These variables that may or may not be used, depending on the command. */
            String username;                        // Username variable. CASE INSENSITIVE IN ALL IMPLEMENTATIONS.
            String soc_value;                       // Current point values on spreadsheet as strings.
            String skc_value;
            String cc_value;
            Integer curr_soc;                       // Current point values on spreadsheet as integers.
            Integer curr_skc;                       // (Don't technically need these; could just parseInt all strings)
            Integer curr_cc;
            Integer new_soc;                        // Updated point values as integers.
            Integer new_skc;
            Integer new_cc;
            String spread_range;                    // Range of user data that is being updated.
            List<List<Object>> new_user_data;       // Actual user data that is being updated.
            Integer user_index = 0;                 // Spot in list where the user data is; see finduser() method

            // Takes next line of input.
            System.out.println("Enter Command: ");
            String userinput = scan.next();

            // Command branching.
            switch (userinput) {

                /* * * * * * * * * * * * * * * * * * * * * * *
                 * * * Done command, terminates program. * *
                 * * * * * * * * * * * * * * * * * * * * * * */
                case "done":
                    return;

                /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
                 * Getpoints command, gets all points for a specific user. * * * * * * * * * * * * * * * * * * * * *
                 * Needs to be in the format "getpoints username" where username corresponds to the needed user. * *
                 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
                case "getpoints":

                    // Checks for valid syntax. Doesn't work for stdin but should work for other things. (?)
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }

                    // Debugging message, mostly. The only necessary line is the second.
                    //System.out.print("Attempting to get points for user: ");
                    username = scan.next();
                    // System.out.println(username);

                    // Finds the user and gets their points.
                    user_index = findUser(values, username);
                    if (user_index == -1) {
                        System.out.println("No user info found!");
                    }
                    else {
                        System.out.println("User \"" + username + "\" has the following points:");
                        System.out.println("Social Credit = " + values.get(user_index).get(4));
                        System.out.println("Skill Credit = " + values.get(user_index).get(5));
                        System.out.println("Communication Credit = " + values.get(user_index).get(6));
                    }

                    break;

                /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
                 * Addpoints commands, add the specified number of each type of point. * * * * * * * *
                 * Needs to be in the format "addpoints username SOC SKC CC" to work properly. * * * *
                 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
                case "addpoints":

                    // Checks for valid syntax. Doesn't work for stdin but should work for other things.
                    // Checks username.
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }
                    else {
                        username = scan.next();
                    }

                    // Checks SOC value (doesn't check for integer).
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }
                    else {
                        soc_value = scan.next();
                        if (soc_value.matches("[0-9]*")) {
                            new_soc = Integer.parseInt(soc_value);
                        }
                        else {
                            System.out.println("INVALID SYNTAX");
                            break;
                        }
                    }

                    // Checks SKC value (doesn't check for integer).
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }
                    else {
                        skc_value = scan.next();
                        if (skc_value.matches("[0-9]*")) {
                            new_skc = Integer.parseInt(skc_value);
                        }
                        else {
                            System.out.println("INVALID SYNTAX");
                            break;
                        }
                    }

                    // Checks CC value (doesn't check for integer).
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }
                    else {
                        cc_value = scan.next();
                        if (cc_value.matches("[0-9]*")) {
                            new_cc = Integer.parseInt(cc_value);
                        }
                        else {
                            System.out.println("INVALID SYNTAX");
                            break;
                        }
                    }

                    // Finds the user and then updates their points.
                    user_index = findUser(values, username);
                    if (user_index == -1) {
                        System.out.println("User not found!");
                    }
                    else {

                        // Gets the current points for the user.
                        curr_soc = Integer.parseInt(values.get(user_index).get(4).toString());
                        new_soc += curr_soc;
                        curr_skc = Integer.parseInt(values.get(user_index).get(5).toString());
                        new_skc += curr_skc;
                        curr_cc = Integer.parseInt(values.get(user_index).get(6).toString());
                        new_cc += curr_cc;

                        // Sets up the new data for communication with the spreadsheet.
                        spread_range = "B" + (user_index + 23) + ":H" + (user_index + 23);
                        new_user_data = values.subList(user_index, user_index + 1);
                        new_user_data.get(0).set(4, new_soc);
                        new_user_data.get(0).set(5, new_skc);
                        new_user_data.get(0).set(6, new_cc);

                        // Debugging line.
                        System.out.println("Updating spreadsheet...");

                        // Updates the spreadsheet.
                        ValueRange body = new ValueRange().setValues(new_user_data);
                        UpdateValuesResponse result = service.spreadsheets().values()
                                .update(spreadsheetId, spread_range, body).setValueInputOption("RAW")
                                .execute();

                    }

                    break;

               /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
                * Changepoints command, changes points to specified numbers. * * * * * * * * * * * *
                * Needs to be in the format "changepoints username SOC SKC CC" to work properly.  * *
                * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
                case "changepoints":
                    // Checks for valid syntax. Doesn't work for stdin but should work for other things.
                    // Checks username.
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }
                    else {
                        username = scan.next();
                    }

                    // Checks SOC value (doesn't check for integer).
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }
                    else {
                        soc_value = scan.next();
                        if (soc_value.matches("[0-9]*")) {
                            new_soc = Integer.parseInt(soc_value);
                        }
                        else {
                            System.out.println("INVALID SYNTAX");
                            break;
                        }
                    }

                    // Checks SKC value (doesn't check for integer).
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }
                    else {
                        skc_value = scan.next();
                        if (skc_value.matches("[0-9]*")) {
                            new_skc = Integer.parseInt(skc_value);
                        }
                        else {
                            System.out.println("INVALID SYNTAX");
                            break;
                        }
                    }

                    // Checks CC value (doesn't check for integer).
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }
                    else {
                        cc_value = scan.next();
                        if (cc_value.matches("[0-9]*")) {
                            new_cc = Integer.parseInt(cc_value);
                        }
                        else {
                            System.out.println("INVALID SYNTAX");
                            break;
                        }
                    }

                    // Finds the user that needs to have points changed.
                    user_index = findUser(values, username);
                    if (user_index == -1) {
                        System.out.println("User not found!");
                    }
                    else {

                        // Sets up the new data for communication with the spreadsheet.
                        spread_range = "B" + (user_index + 23) + ":H" + (user_index + 23);
                        new_user_data = values.subList(user_index, user_index + 1);
                        new_user_data.get(0).set(4, new_soc);
                        new_user_data.get(0).set(5, new_skc);
                        new_user_data.get(0).set(6, new_cc);

                        // Debugging line.
                        System.out.println("Updating spreadsheet...");

                        // Updates the spreadsheet.
                        ValueRange body = new ValueRange().setValues(new_user_data);
                        UpdateValuesResponse result = service.spreadsheets().values()
                                .update(spreadsheetId, spread_range, body).setValueInputOption("RAW")
                                .execute();

                    }

                    break;

               /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
                * Checkpromo command, checks available promotions.  * * * * * * * * * * * * * * *
                * Needs to be in the format "changepoints username" where USERNAME CAN BE "ALL" *
                * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
                case "checkpromo":

                    // Checks for valid syntax. Doesn't work for stdin but should work for other things. (?)
                    if (!scan.hasNext()) {
                        System.out.println("INVALID SYNTAX");
                        break;
                    }

                    // Gets the username and prints it out for debugging.
                    username = scan.next();
                    System.out.println("Checking promotions for " + username + "...");

                    // SPECIAL CASE: Checks for all promotions. Essentially the else case but for every element.
                    if (username.toUpperCase().equals("ALL")) {
                        checkAllPromotions(values);
                    }
                    else {

                        // Finds the user in the spreadsheet.
                        user_index = findUser(values, username);
                        if (user_index == -1) {
                            System.out.println("User not found!");
                        }
                        else {

                            // Pulls the current points for the selected user.
                            curr_soc = Integer.parseInt(values.get(user_index).get(4).toString());
                            curr_skc = Integer.parseInt(values.get(user_index).get(5).toString());
                            curr_cc = Integer.parseInt(values.get(user_index).get(6).toString());

                            // Finds the current SoC rank.
                            int highest_soc = 0;
                            for (int s = 0; s < PROMOTION_SOC_VALUES.length; s++) {
                                if (curr_soc < PROMOTION_SOC_VALUES[s]) {
                                    highest_soc = s - 1;
                                    break;
                                }
                                else {
                                    if (s == PROMOTION_SOC_VALUES.length - 1) {
                                        highest_soc = s;
                                        break;
                                    }
                                }
                            }

                            // Finds the current SkC rank.
                            int highest_skc = 0;
                            for (int s = 0; s < PROMOTION_SKC_VALUES.length; s++) {
                                if (curr_skc < PROMOTION_SKC_VALUES[s]) {
                                    highest_skc = s - 1;
                                    break;
                                }
                                else {
                                    if (s == PROMOTION_SKC_VALUES.length - 1) {
                                        highest_skc = s;
                                        break;
                                    }
                                }
                            }

                            // Finds the current CC rank.
                            int highest_cc = 0;
                            for (int s = 0; s < PROMOTION_CC_VALUES.length; s++) {
                                if (curr_cc < PROMOTION_CC_VALUES[s]) {
                                    highest_cc = s - 1;
                                    break;
                                }
                                else {
                                    if (s == PROMOTION_CC_VALUES.length - 1) {
                                        highest_cc = s;
                                        break;
                                    }
                                }
                            }

                            // The lowest of the three numbers is the highest promotion.
                            int curr_rank = Math.min(highest_cc, Math.min(highest_skc, highest_soc));
                            String curr_rank_code = PROMOTION_LETTERS[curr_rank];

                            // Checks to see if that number is different than the current one.
                            if (!values.get(user_index).get(1).toString().equals(curr_rank_code)) {
                                System.out.println("User " + username + " should be promoted to " + PROMOTION_NAMES[curr_rank]);
                            }
                            else {
                                System.out.println("User " + username + " should not be promoted.");
                            }

                        }
                    }

                    break;

                // Unknown command default.
                default:
                    System.out.println("Unknown command!");
                    break;
            }

        }
    }
}