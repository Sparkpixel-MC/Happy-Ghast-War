package me.wang.happyGhastWar.util;

import me.wang.happyGhastWar.HappyGhastWar;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Language {
    private final HappyGhastWar main;
    private File file;
    private YamlConfiguration config;
    private final Logger logger;
    private String name;

    public Language(HappyGhastWar ghastWar){
        this.main = ghastWar;
        this.logger = ghastWar.getLogger();
    }

    public void loadLanguage(String name){
        this.name = name;
        try{
            file = new File(main.getDataFolder()+"/languages",name);
            if (!file.exists()){
                main.saveResource("languages/"+name,false);
            }
            config = YamlConfiguration.loadConfiguration(file);
            config.save(file);
            logger.info("Successfully loaded language " + name);
        } catch (IOException e) {
            logger.severe("Failed to load language "+name);
            throw new RuntimeException(e);
        }

    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void reload(){
        if (config == null){
            return;
        }
        try {
            config.load(file);
            logger.info("Successfully reloaded language " + name);
        } catch (InvalidConfigurationException | IOException e) {
            logger.severe("Failed to reload language "+name);
            throw new RuntimeException(e);
        }
    }

    public String getRawContent(String key){
        return config.getString(key);
    }

    /**
     * 获取语言文本（MiniMessage 标签格式）。
     * 旧语言包里的 {@code &x} 色码会自动转换为对应标签；缺失 key 时回退为 key 本身，避免 NPE。
     * 发送时请经 {@link Text#send} / {@link Text#mm}；喂给记分板等 legacy API 前用 {@link Text#legacy}。
     */
    public String getContent(String key){
        String raw = config.getString(key);
        // 语言包缺失该 key 时回退为 key 本身，避免 NPE
        if (raw == null){
            return key;
        }
        return Text.legacyToTag(raw);
    }

    public List<String> getList(String key){
        return config.getStringList(key);
    }

    /** 获取语言文本列表（MiniMessage 标签格式，同 {@link #getContent}） */
    public List<String> getTranslatedList(String key){
        List<String> list = config.getStringList(key);
        return list.stream().map(Text::legacyToTag).collect(Collectors.toList());
    }
}
