package org.kalimbekov;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (
                Scanner scannerFile = new Scanner(new File("data/credentials"));
                Scanner scannerSysIn = new Scanner(System.in)
        ) {
            String sender_address = scannerFile.nextLine();
            String sender_password = scannerFile.nextLine();

            System.out.print("Enter receiver address: ");
            String receiver_address = scannerSysIn.nextLine();

            String weatherInfo = getWeatherInfo();
            String currentDate = getCurrentDate();

            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            Session session = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(sender_address, sender_password);
                }
            });

            // Create a multipart message to handle the attachments
            Multipart multipart = new MimeMultipart();

            // Attach a file
            String[] filePaths = {
                    "assets/cat.jpg",
                    "src/main/java/org/kalimbekov/Main.java"
            };
            for (String filePath : filePaths)
                addAttachment(multipart, filePath);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setText(
                    currentDate + "\n" + weatherInfo,
                    "utf-8"
            );
            multipart.addBodyPart(mimeBodyPart);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender_address));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver_address));
            message.setSubject("09-152, Алимбеков Камиль Риясович");
            message.setContent(multipart);

            Transport.send(message);
        } catch (FileNotFoundException e) {
            System.out.println("""
                    You must create 'credentials' file inside the root directory
                    in order to use this program
                                        
                    'credentials' file contents format:
                    email_address
                    password
                    """);
        } catch (AddressException e) {
            System.out.println("""
                    Address you entered is not a valid one
                    """);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void addAttachment(Multipart multipart, String filePath)
            throws MessagingException {
        DataSource source = new FileDataSource(filePath);

        BodyPart attachmentBodyPart = new MimeBodyPart();
        attachmentBodyPart.setDataHandler(new DataHandler(source));
        attachmentBodyPart.setFileName(source.getName());

        multipart.addBodyPart(attachmentBodyPart);
    }

    public static String getWeatherInfo()
            throws IOException {
        // TODO:
        // It is a TERRIBLE idea to store API keys
        // and other sensitive data directly in a
        // source code, but I will keep it like
        // that for the sake of simplicity
        String apiKey = "d1185afad3325cb06f73a11b95d2c4a4";
        String location = "Kazan";

        String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q="
                + location
                + "&appid="
                + apiKey
                + "&lang=ru";

        CloseableHttpResponse response;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            response = httpClient.execute(request);
        }

        String jsonResponse = EntityUtils.toString(response.getEntity());
        JsonElement element = JsonParser.parseString(jsonResponse);

        if (element.getAsJsonObject().get("cod").getAsInt() != 200) {
            return "не удаётся получить информацию от OpenWeatherMap API";
        }

        String description = element.
                getAsJsonObject().
                get("weather").
                getAsJsonArray().
                get(0).
                getAsJsonObject().
                get("description").
                getAsString();
        float temperature = element.
                getAsJsonObject().
                get("main").
                getAsJsonObject().
                get("temp").
                getAsFloat();
        float wind = element.
                getAsJsonObject().
                get("wind").
                getAsJsonObject().
                get("speed").
                getAsFloat();

        // Погода в Казани: облачно, 17 градусов Цельсия, 3 м/с
        return String.format("Погода в Казани: %s, %.0f градусов Цельсия, %.0f м/с",
                description,
                temperature,
                wind);
    }

    public static String getCurrentDate() {
        // Create a Calendar instance and set it to the current date
        Calendar calendar = Calendar.getInstance();

        // Get the day of the week (Sunday = 1, Monday = 2, ..., Saturday = 7)
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // Create a SimpleDateFormat instance with the desired pattern and Russian locale
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy EEEE", Locale.forLanguageTag("ru"));

        // Set the calendar's day of the week to the current day
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);

        // Format the date to get the day of the week in Russian
        return sdf.format(calendar.getTime());
    }
}
