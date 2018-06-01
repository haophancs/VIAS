package com.haophan.vias.weather.Model;

/**
 * Created by USER on 11/3/2017.
 */

public class Weather {
    private int id;
    private String main;
    private String description;
    private String icon;

    private static final String[] vie_description = {"bão với mưa nhẹ", "có bão và mưa", "bão với mưa lớn", "sấm sét nhẹ", "có dông", "có bão lớn", "có dông bão", "bão với mưa phùn", "bão với mưa phùn", "bão với mưa lớn", "mưa phùn nhẹ", "mưa phùn", "mưa lớn nặng hạt", "mưa phùn lớn", "mưa phùn", "mưa phùn mưa lớn", "mưa rào và mưa phùn", "mưa lớn và mưa phùn", "mưa phùn", "mưa nhỏ", "mưa vừa", "mưa nặng hạt", "mưa rất lớn", "mưa cực đoan", "mưa đá", "mưa rào cường độ thấp", "mưa rào", "mưa rào nặng hạt", "mưa không đều", "tuyết nhẹ", "tuyết", "tuyết rơi dày đặc", "mưa tuyết", "mưa tuyết", "mưa nhẹ và tuyết", "mưa và tuyết", "tuyết rơi nhẹ", "tuyết bất chợt", "tuyết và mưa nặng hạt", "sương mù", "có khói", "sương mù vào buổi sáng", "cát và bụi xoáy", "sương mù", "cát bay", "bụi bặm", "tro núi lửa", "gió mạnh", "vòi rồng", "trời quang đãng", "vài đám mây", "mây phân tán", "mây rải rác", "mây đen", "vòi rồng", "bão nhiệt đới", "có bão", "trời lạnh", "trời nóng bức", "trời gió", "gió mạnh kèm mưa", "êm dịu, không có gió", "gió nhẹ", "gió nhẹ", "gió vừa", "gió nhẹ", "gió mạnh", "gió lớn và gần cơn bão", "dông lốc", "bão lớn", "bão táp", "bão dữ dội", "cuồng phong"};
    private static final String[] eng_description = {"thunderstorm with light rain", "thunderstorm with rain", "thunderstorm with heavy rain", "light thunderstorm", "thunderstorm", "heavy thunderstorm", "ragged thunderstorm", "thunderstorm with light drizzle", "thunderstorm with drizzle", "thunderstorm with heavy drizzle", "light intensity drizzle", "drizzle", "heavy intensity drizzle", "light intensity drizzle rain", "drizzle rain", "heavy intensity drizzle rain", "shower rain and drizzle", "heavy shower rain and drizzle", "shower drizzle", "light rain", "moderate rain", "heavy intensity rain", "very heavy rain", "extreme rain", "freezing rain", "light intensity shower rain", "shower rain", "heavy intensity shower rain", "ragged shower rain", "light snow", "snow", "heavy snow", "sleet", "shower sleet", "light rain and snow", "rain and snow", "light shower snow", "shower snow", "heavy shower snow", "mist", "smoke", "haze", "sand, dust whirls", "fog", "sand", "dust", "volcanic ash", "squalls", "tornado", "clear sky", "few clouds", "scattered clouds", "broken clouds", "overcast clouds", "tornado", "tropical storm", "hurricane", "cold", "hot", "windy", "hail", "calm", "light breeze", "gentle breeze", "moderate breeze", "fresh breeze", "strong breeze", "high wind, near gale", "gale", "severe gale", "storm", "violent storm", "hurricane"};

    public Weather(int id, String main, String description, String icon) {
        this.id = id;
        this.main = main;
        this.description = description;
        this.icon = icon;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getDescription() {
        String d = description;
        for (int i = 0; i < vie_description.length; i++){
            if (d.contains(vie_description[i])) {
                d = eng_description[i];
                break;
            }
        }
        return d;
    }

    public String getVieDescription(){
        description = description.replace("bầu trời quang đãng", "clear sky");
        String d = description;

        for (int i = 0; i < eng_description.length; i++){
            if (d.contains(eng_description[i])) {
                d = vie_description[i];
                break;
            }
        }
        return d;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
