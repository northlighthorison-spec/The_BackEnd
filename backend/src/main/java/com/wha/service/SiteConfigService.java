package com.wha.service;

import com.wha.entity.SiteConfig;
import com.wha.repository.SiteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final SiteConfigRepository repo;

    private static final List<SiteConfig> DEFAULTS = List.of(
        SiteConfig.builder().key("stat_people_helped").value("30,000+")
            .label("People helped (headline stat)").type("text").build(),
        SiteConfig.builder().key("stat_people_sub").value("and counting")
            .label("People helped (sub-label)").type("text").build(),
        SiteConfig.builder().key("stat_families").value("2,000+")
            .label("Families supported (headline stat)").type("text").build(),
        SiteConfig.builder().key("stat_families_sub").value("with direct relief")
            .label("Families supported (sub-label)").type("text").build(),
        SiteConfig.builder().key("stat_third_value").value("100%")
            .label("Third stat value").type("text").build(),
        SiteConfig.builder().key("stat_third_label").value("Independent")
            .label("Third stat label").type("text").build(),
        SiteConfig.builder().key("stat_third_sub").value("no government funding")
            .label("Third stat sub-label").type("text").build(),
        SiteConfig.builder().key("stat_footnote")
            .value("Including emergency response to flood victims and ongoing community aid programmes.")
            .label("Stats section footnote").type("text").build(),
        SiteConfig.builder().key("hero_headline").value("World Humanitarian Aid")
            .label("Homepage hero headline").type("text").build(),
        SiteConfig.builder().key("hero_body")
            .value("An independent charity working to deliver direct relief to people who need it most. We are not funded by any government. We answer to no one but the people we serve.")
            .label("Homepage hero body text").type("textarea").build(),
        SiteConfig.builder().key("donate_cta_headline").value("Support the work directly")
            .label("Donate CTA headline").type("text").build(),
        SiteConfig.builder().key("donate_cta_body")
            .value("Donations go straight to operations. No large administrative overhead, no celebrity spokespeople — just the work.")
            .label("Donate CTA body text").type("textarea").build(),
        SiteConfig.builder().key("contact_phone").value("")
            .label("Contact phone number").type("text").build()
    );

    public Map<String, String> getAllAsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SiteConfig d : DEFAULTS) {
            map.put(d.getKey(), d.getValue());
        }
        for (SiteConfig saved : repo.findAll()) {
            map.put(saved.getKey(), saved.getValue());
        }
        return map;
    }

    public List<SiteConfig> getAllWithMeta() {
        Map<String, SiteConfig> result = new LinkedHashMap<>();
        for (SiteConfig d : DEFAULTS) {
            result.put(d.getKey(), d);
        }
        for (SiteConfig saved : repo.findAll()) {
            SiteConfig merged = result.getOrDefault(saved.getKey(), saved);
            merged.setValue(saved.getValue());
            result.put(saved.getKey(), merged);
        }
        return List.copyOf(result.values());
    }

    @Transactional
    public void update(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue().strip();
            if (value.isEmpty()) continue;

            SiteConfig config = repo.findById(key).orElseGet(() -> {
                SiteConfig c = new SiteConfig();
                c.setKey(key);
                c.setType("text");
                return c;
            });
            config.setValue(value);
            repo.save(config);
        }
    }
}
