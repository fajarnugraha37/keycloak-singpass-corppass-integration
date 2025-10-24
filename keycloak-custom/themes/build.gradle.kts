plugins { java }
group = "com.mycorp.keycloak"
version = "1.0.0"

java { 
    toolchain { 
        languageVersion.set(JavaLanguageVersion.of(17)) 
    } 
}

tasks.withType<Jar> {
    from("theme")
    archiveBaseName.set("custom-keycloak-themes")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

repositories { 
    mavenCentral() 
}

dependencies { 
    /* none */ 
}
