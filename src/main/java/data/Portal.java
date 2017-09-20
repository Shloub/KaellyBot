package data;

import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by steve on 07/11/2016.
 */
public class Portal implements Embedded{

    private final static Logger LOG = LoggerFactory.getLogger(Portal.class);
    private final static long LIMIT = 172800000;
    private final static DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy à HH:mm", Locale.FRANCE);
    private final static DateFormat dayFormat = new SimpleDateFormat("à HH:mm", Locale.FRANCE);
    private final static Pattern sweetDateFormat = Pattern.compile("(\\d+\\s+j\\s+)?(\\d+\\s+h\\s+)?(\\d+\\s+min)");

    private static Map<String, Portal> portals;

    private String name;
    private String url;
    private String coordonate;
    private int utilisation;
    private long creation;
    private long lastUpdate;
    private int color;
    private Guild guild;

    private Portal(String name, String url, int color) {
        this.name = name;
        this.url = url;
        this.color = color;
    }

    private Portal(String name, String coordonate, int utilisation, long creation, long lastUpdate) {
        this.name = name;
        if (coordonate != null)
            this.coordonate = coordonate;
        else
            this.coordonate = "";
        this.utilisation = utilisation;
        this.creation = creation;
        this.lastUpdate = lastUpdate;
    }

    private Portal(Portal classicPortal, String coordonate, int utilisation, long creation, long lastUpdate, Guild guild) {
        this.name = classicPortal.getName();
        this.url = classicPortal.getUrl();
        this.color = classicPortal.getColor();
        if (coordonate != null)
            this.coordonate = coordonate;
        else
            this.coordonate = "";
        this.utilisation = utilisation;
        this.creation = creation;
        this.lastUpdate = lastUpdate;
        this.guild = guild;
    }

    public static Map<String, Portal> getPortals(){
        if (portals == null){
            portals = new HashMap<>();

            Connexion connexion = Connexion.getInstance();
            Connection connection = connexion.getConnection();

            try {
                PreparedStatement query = connection.prepareStatement("SELECT name, url, color FROM Portal");
                ResultSet resultSet = query.executeQuery();

                while (resultSet.next()) {
                    Portal portal = new Portal(resultSet.getString("name"),
                            resultSet.getString("url"),
                            resultSet.getInt("color"));
                    portals.put(portal.getName(), portal);
                }
            } catch (SQLException e) {
                ClientConfig.setSentryContext(null,null, null, null);
                LOG.error(e.getMessage());
            }
        }

        return portals;
    }

    public static List<Portal> getPortals(Guild g){
        List<Portal> portals = new ArrayList<>();

        Connexion connexion = Connexion.getInstance();
        Connection connection = connexion.getConnection();

        try {
            PreparedStatement query = connection.prepareStatement("SELECT name_portal, pos, utilisation,"
                    + "creation, last_update FROM Portal_Guild WHERE id_guild = (?);");
            query.setString(1, g.getId());
            ResultSet resultSet = query.executeQuery();

            String name;
            String pos;
            int utilisation;
            long creation;
            long lastUpdate;

            while (resultSet.next()) {
                name = resultSet.getString("name_portal");
                pos = resultSet.getString("pos");
                utilisation = resultSet.getInt("utilisation");
                creation = resultSet.getLong("creation");
                lastUpdate = resultSet.getLong("last_update");
                portals.add(new Portal(Portal.getPortals().get(name), pos, utilisation, creation, lastUpdate, g));
            }

        } catch (SQLException e) {
            ClientConfig.setSentryContext(ClientConfig.DISCORD().getGuildByID(Long.parseLong(g.getId())),
                    null, null, null);
            LOG.error(e.getMessage());
        }

        return portals;
    }

    public String getName() {
        return this.name;
    }

    public void setUtilisation(int utilisation) {
        setUtilisation(utilisation, System.currentTimeMillis());
    }
    public void setUtilisation(int utilisation, long lastUpdate) {
        this.utilisation = utilisation;
        this.lastUpdate = lastUpdate;

        Connexion connexion = Connexion.getInstance();
        Connection connection = connexion.getConnection();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "UPDATE Portal_Guild SET utilisation = ?, last_update = ?"
                            + "WHERE name_portal = ? AND id_guild = ?;");
            preparedStatement.setInt(1, utilisation);
            preparedStatement.setLong(2, lastUpdate);
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, guild.getId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            ClientConfig.setSentryContext(ClientConfig.DISCORD().getGuildByID(Long.parseLong(guild.getId())),
                    null, null, null);
            LOG.error(e.getMessage());
        }
    }

    public void setCoordonate(String coordonate) {
        setCoordonate(coordonate, System.currentTimeMillis());
    }

    public void setCoordonate(String coordonate, long creation) {
        if (! this.coordonate.equals(coordonate)){
            if (coordonate != null)
                this.coordonate = coordonate;
            else
                this.coordonate = "";
            this.creation = creation;
            this.utilisation = -1;
            this.lastUpdate = -1;

            Connexion connexion = Connexion.getInstance();
            Connection connection = connexion.getConnection();

            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "UPDATE Portal_Guild SET pos = ?, creation = ?, utilisation = ?, last_update = ?"
                                + "WHERE name_portal = ? AND id_guild = ?;");
                preparedStatement.setString(1, coordonate);
                preparedStatement.setLong(2, creation);
                preparedStatement.setInt(3, utilisation);
                preparedStatement.setLong(4, lastUpdate);
                preparedStatement.setString(5, name);
                preparedStatement.setString(6, guild.getId());

                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                ClientConfig.setSentryContext(ClientConfig.DISCORD().getGuildByID(Long.parseLong(guild.getId())),
                        null, null, null);
                LOG.error(e.getMessage());
            }
        }
    }

    public void merge(Portal portal){

        if (Normalizer.normalize(portal.getName(), Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase()
                        .equals(Normalizer.normalize(getName(), Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase())
                && ! portal.coordonate.equals(""))
            if (coordonate.equals(portal.coordonate) && utilisation > portal.utilisation || coordonate.equals("")){
                setCoordonate(portal.coordonate, portal.creation);
                setUtilisation(portal.utilisation, portal.lastUpdate);
            }
            else if (! coordonate.equals("") && ! coordonate.equals(portal.coordonate) && creation < portal.creation){
                setCoordonate(portal.coordonate, portal.creation);
                setUtilisation(portal.utilisation, portal.lastUpdate);
            }
    }

    public void reset() {
        setCoordonate(null);
    }

    @Override
    public EmbedObject getEmbedObject() {
        EmbedBuilder builder = new EmbedBuilder();

        builder.withTitle(name);
        builder.withColor(getColor());
        builder.withThumbnail(url);

        if (System.currentTimeMillis() - creation > LIMIT
                ||coordonate.equals("")) {
            coordonate = "";
            utilisation = -1;
            builder.withDescription("Aucune position récente trouvée.");
        } else {
            builder.appendField(":cyclone: Position", "**" + coordonate + "**", true);
            if (utilisation != -1)
                builder.appendField(":eye: Utilisation", utilisation + " utilisation"
                        + (utilisation > 1 ? "" : "s"), true);
            builder.withFooterText(getDateInformation());
        }

        return builder.build();
    }

    @Override
    public String toString(){
        StringBuilder st = new StringBuilder("*")
                .append(getName())
                .append( "* : ");
        if (System.currentTimeMillis() - creation > LIMIT
                ||coordonate.equals("")) {
            coordonate = "";
            utilisation = -1;
            st.append("Aucune position récente trouvée.\n");
        }
        else {
            st.append("**").append(coordonate).append("**");

            if (utilisation != -1)
                st.append(", ").append(utilisation).append(" utilisations");
            st.append(getDateInformation()).append(")*\n");
        }

        return st.toString();
    }

    public static List<Portal> getSweetPortals(ServerDofus server) throws IOException {
        List<Portal> portals = new ArrayList<>();

        Document doc = JSoupManager.getDocument(Constants.sweetPortals + server.getSweetId());
        Elements dimensions = doc.getElementsByClass("row");

        for(Element dim : dimensions){
            String name = dim.getElementsByTag("h2").get(0).text();
            String coordonate = dim.getElementsByTag("h2").get(1).text();
            int utilisation = -1;
            long creation = 0;

            if (! coordonate.equals("Position Inconnue")) {
                coordonate = dim.getElementsByTag("h3").get(0).text();
                utilisation = Integer.parseInt(dim.getElementsByTag("h3")
                        .get(1).getElementsByTag("b").get(0).text());

                Matcher m = sweetDateFormat.matcher(dim.getElementsByTag("h3").get(3).text());
                m.find();
                long timeToRemove = 0;
                if (m.group(1) != null)
                    timeToRemove +=  86400000 * Integer.parseInt(m.group(1).replaceAll("\\s+j\\s+", ""));
                if (m.group(2) != null)
                    timeToRemove += 3600000 * Integer.parseInt(m.group(2).replaceAll("\\s+h\\s+", ""));
                timeToRemove += 60000 * Integer.parseInt(m.group(3).replaceAll("\\s+min", ""));

                creation = System.currentTimeMillis() - timeToRemove;
            }
            else
                coordonate = "";
            long lastUpdate = creation;
            portals.add(new Portal(name, coordonate, utilisation, creation, lastUpdate));
        }

        return portals;
    }

    private String getDateInformation(){
        StringBuilder st = new StringBuilder("Ajouté ");
        Date creationDate = new Date(creation);
        if (! DateUtils.isSameDay(creationDate, new Date()))
            st.append("le ").append(dateFormat.format(creationDate));
        else
            st.append(dayFormat.format(creationDate));
        if (lastUpdate != -1) {
            Date updateDate = new Date(lastUpdate);
            if (! DateUtils.truncatedEquals(creationDate, updateDate, Calendar.SECOND)) {
                st.append(" - édité ");
                if (! DateUtils.isSameDay(updateDate, new Date()))
                    st.append("le ").append(dateFormat.format(updateDate));
                else
                    st.append(dayFormat.format(updateDate));
            }
        }
        return st.toString();
    }

    public String getUrl() {
        return url;
    }

    public int getColor() {
        return color;
    }
}
