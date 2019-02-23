public class Settings {

    // Токен Telegram бота
    public String tgToken = "";

    // ID чата в ТГ
    public long chatId = -333530356;

    // Прокси для отправки сообщения в Telegram
    public String proxyTelegram = "";

    // Минута матча, для отправки сообщения
    public int TimeSelectMin = 45;
    public int TimeSelectMax = 45;

    // Владение мячом, минимум
    public int possessionMin = 60;

    // Удар по воротам, минимум
    public int TargetOnMin = 0;

    // Удар мимо ворот, минимум
    public int TargetOffMin = 0;

    // И/ИЛИ
    public Logic logic = Logic.AND;

    // Список прокси для сайта
    //private List<String> lstProxy =new ArrayList<String>();

    //Тайм-аут прокси, мс
    public int timeout = 30000;

    // Логика условий
    public enum Logic {
        AND,
        OR
    }
}