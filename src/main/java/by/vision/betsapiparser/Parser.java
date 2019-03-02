package by.vision.betsapiparser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Parser {
    //static Settings settings = new Settings();
    String ip;
    int port;
    Proxy proxy;

    Parser(Proxy proxy) {
        this.proxy = proxy;
        ip = proxy.getIp();
        port = proxy.getPort();

    }


    void parseMainPage() {

        final String HOST_SITE = "https://ru.betsapi.com";
        //read from url
        final String MAIN_URL = "https://ru.betsapi.com/ci/soccer";
        System.out.println("proxy " + ip + ":" + port);

        Elements scopeElements;
        List<MainPageInfo> mainPageInfoList = new ArrayList<>();

        //Setting up connection
        Document doc = null;
        while (doc == null){
            doc = getDoc(MAIN_URL);
        }

        //selecting scope of elements, where needed information located
        scopeElements = doc.select("table[id=tbl_inplay] tr");

        //parse values from scope and write it to mainPageInfoList
        for (Element e : scopeElements
        ) {
            MainPageInfo mainPageInfo = new MainPageInfo();

            //league
            mainPageInfo.setLeague(e.selectFirst("td[class=league_n] a").ownText());
            //matchId
            mainPageInfo.setIdMatch(e.attr("id").replace("r_", ""));
            //time
            Element timeElement = e.selectFirst("span[class=race-time]");
            mainPageInfo.setTime(Integer.parseInt(timeElement.ownText().replace("\'", "")));
            //timesup
            Element timeSupElement = timeElement.selectFirst("sup");
            if (timeSupElement != null) {
                timeSupElement.remove();
            }
            //score
            Element scoreElement = e.selectFirst(String.format("td[id=o_%s_0]", mainPageInfo.getIdMatch()));
            mainPageInfo.setScore(scoreElement.ownText());
            //URL
            mainPageInfo.setUrlMatch(e.select("td[class=text-center] a").attr("href"));
            //rate
            mainPageInfo.setRateL(e.selectFirst(String.format("td[id=o_%s_0]", mainPageInfo.getIdMatch())).ownText());
            mainPageInfo.setRateC(e.selectFirst(String.format("td[id=o_%s_1]", mainPageInfo.getIdMatch())).ownText());
            mainPageInfo.setRateR(e.selectFirst(String.format("td[id=o_%s_2]", mainPageInfo.getIdMatch())).ownText());
            //clubs names
            mainPageInfo.setClubL(e.selectFirst("td[class=text-right text-truncate] a").ownText());
            mainPageInfo.setClubR(e.selectFirst("td[class=text-truncate] a").ownText());

            mainPageInfoList.add(mainPageInfo);

        }

        //remove matches from list , that don't meet time value
        mainPageInfoList.removeIf(s -> (s.getTime() < Settings.timeSelectMin || s.getTime() > Settings.timeSelectMax));

        //parse all compatible matches sites
        if (mainPageInfoList.size() > 0) {
            for (MainPageInfo info : mainPageInfoList
            ) {
                //assemble url
                info.setUrlMatch(HOST_SITE + info.getUrlMatch());
                parseMatchPage(info.getUrlMatch());

                MatchInfo leftMatch = MainPageInfo.getMatchInfoL();
                MatchInfo rightMatch = MainPageInfo.getMatchInfoR();

                switch (Settings.logic) {
                    case OR:
                        if (leftMatch.getPossession() >= Settings.possessionMin ||
                                leftMatch.getTargetOn() >= Settings.targetOnMin ||
                                leftMatch.getTargetOff() >= Settings.targetOffMin) break;
                        if (rightMatch.getPossession() >= Settings.possessionMin ||
                                rightMatch.getTargetOn() >= Settings.targetOnMin ||
                                rightMatch.getTargetOff() >= Settings.targetOffMin) break;
                        continue;
                    case AND:
                        if (leftMatch.getPossession() >= Settings.possessionMin &&
                                leftMatch.getTargetOn() >= Settings.targetOnMin &&
                                leftMatch.getTargetOff() >= Settings.targetOffMin) break;
                        if (rightMatch.getPossession() >= Settings.possessionMin &&
                                rightMatch.getTargetOn() >= Settings.targetOnMin &&
                                rightMatch.getTargetOff() >= Settings.targetOffMin) break;
                        continue;

                }

                sendTelegramMessage(info, leftMatch, rightMatch);

            }
        }


    }

    public void parseMatchPage(String site) {

        //handle NullPointerException
        Document doc = null;
        while (doc == null){
            doc = getDoc(site);
        }

        Elements infoInTr = doc.select("table.table-sm tr");
        //remove <span class="sr-only"> because of repetition parameters with <div ... role="progressbar"...>
        infoInTr.select("span.sr-only").remove();

        List<String[]> paramList = new ArrayList<>();

        //TODO shit code. can be better.
        for (Element trElement : infoInTr
        ) {
            Elements tdTagElements = trElement.select("td");
            int counter = 0;
            String[] paramInfoBlock = new String[3];
            for (Element elem : tdTagElements
            ) {
                paramInfoBlock[counter] = elem.text();
                counter++;
                if (counter == 3) paramList.add(paramInfoBlock);
            }
        }


        for (String[] param : paramList
        ) {
            String header = param[1];
            String leftTeamParam = param[0];
            String rightTeamParam = param[2];

            MatchInfo leftMatch = MainPageInfo.getMatchInfoL();
            MatchInfo rightMatch = MainPageInfo.getMatchInfoR();

            switch (header) {
                case "":
                    leftMatch.setClubName(param[0]);
                    rightMatch.setClubName(param[2]);
                    break;

                case "Goals":
                    leftMatch.setGoals(Integer.parseInt(leftTeamParam));
                    rightMatch.setGoals(Integer.parseInt(rightTeamParam));
                    break;

                case "Corners":
                    leftMatch.setCorners(Integer.parseInt(leftTeamParam));
                    rightMatch.setCorners(Integer.parseInt(rightTeamParam));
                    break;

                case "Corners (Half)":
                    leftMatch.setCornersHalf(Integer.parseInt(leftTeamParam));
                    rightMatch.setCornersHalf(Integer.parseInt(rightTeamParam));
                    break;

                case "Желтые карточки":
                    leftMatch.setCardYellow(Integer.parseInt(leftTeamParam));
                    rightMatch.setCardYellow(Integer.parseInt(rightTeamParam));
                    break;

                case "Красные карточки":
                    leftMatch.setCardRed(Integer.parseInt(leftTeamParam));
                    rightMatch.setCardRed(Integer.parseInt(rightTeamParam));
                    break;

                case "Пенальти":
                    leftMatch.setPenalties(Integer.parseInt(leftTeamParam));
                    rightMatch.setPenalties(Integer.parseInt(rightTeamParam));
                    break;

                case "Замены":
                    leftMatch.setSubstitutions(Integer.parseInt(leftTeamParam));
                    rightMatch.setSubstitutions(Integer.parseInt(rightTeamParam));
                    break;

                case "Атака":
                    leftMatch.setAttacks(Integer.parseInt(leftTeamParam));
                    rightMatch.setAttacks(Integer.parseInt(rightTeamParam));
                    break;

                case "Опасная Атака":
                    leftMatch.setAttacksDangerous(Integer.parseInt(leftTeamParam));
                    rightMatch.setAttacksDangerous(Integer.parseInt(rightTeamParam));
                    break;


                case "Удары в створ ворот":
                    leftMatch.setTargetOn(Integer.parseInt(leftTeamParam));
                    rightMatch.setTargetOn(Integer.parseInt(rightTeamParam));
                    break;

                case "Удары мимо ворот":
                    leftMatch.setTargetOff(Integer.parseInt(leftTeamParam));
                    rightMatch.setTargetOff(Integer.parseInt(rightTeamParam));
                    break;

                case "Владение мячом":
                    leftMatch.setPossession(Integer.parseInt(leftTeamParam));
                    rightMatch.setPossession(Integer.parseInt(rightTeamParam));
                    break;

            }
        }

    }

    private Document getDoc(String URL) {
        /*try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        int statusCode = 0;
        int numTries = 5;
        Connection.Response response = null;
        while (true) {
            try {
                response = Jsoup.connect(URL)
                        .timeout(Settings.proxyTimeout)
                        .proxy(ip, port)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36")
                        .referrer("http://www.google.com")
                        //.ignoreHttpErrors(true)
                        .execute();
                statusCode = response.statusCode();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                if (--numTries <= 0) try {
                    throw e;
                } catch (IOException e1) {
                    e1.printStackTrace();
                    proxy.refresh();
                    ip = proxy.getIp();
                    port = proxy.getPort();
                    System.out.println("Status code "+statusCode);
                    System.out.println("Proxy "+ip+":"+port);
                }
            }
        }

        Document doc = null;
        try {
            doc = response.parse();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return doc;
        }
    }

    private static void sendTelegramMessage(MainPageInfo info, MatchInfo leftMatch, MatchInfo rightMatch) {
        StringBuilder stringBuilder = new StringBuilder();
        String newLine = System.lineSeparator();
        stringBuilder.append("<b>").append(info.getLeague()).append("</b>").append(newLine)
                .append(leftMatch.getClubName()).append(" (").append(info.getRateL()).append(") - ").append(rightMatch.getClubName()).append(" (").append(info.getRateR()).append(")").append(newLine)
                .append("<i>").append(info.getTime()).append(" мин.</i>").append(newLine)
                .append("<b>").append(info.getScore()).append("</b>").append(newLine)
                .append("АТ (атаки): [").append(leftMatch.getAttacks()).append(", ").append(rightMatch.getAttacks()).append("]").append(newLine)
                .append("ОАТ (опасные атаки): [").append(leftMatch.getAttacksDangerous()).append(", ").append(rightMatch.getAttacksDangerous()).append("]").append(newLine)
                .append("В (владение мячем): [").append(leftMatch.getPossession()).append(", ").append(rightMatch.getPossession()).append("]").append(newLine)
                .append("У (угловые): [").append(leftMatch.getCorners()).append(", ").append(rightMatch.getCorners()).append("]").append(newLine)
                .append("УВ (удары в створ): [").append(leftMatch.getTargetOn()).append(", ").append(rightMatch.getTargetOn()).append("]").append(newLine)
                .append("УМ (удары мимо ворот): [").append(leftMatch.getTargetOff()).append(", ").append(rightMatch.getTargetOff()).append("]").append(newLine)
                .append(info.getUrlMatch());

        TelegramBot telegramBot = new TelegramBot();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(Settings.tgChatID).setParseMode("html").setText(stringBuilder.toString());
        try {
            telegramBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
