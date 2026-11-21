package in.ac.bitspilani.triagegraph.triage.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.ac.bitspilani.triagegraph.triage.domain.KeywordRule;

/** Starting keyword rules, written to the database the first time triage runs. Admins edit them afterwards. */
final class DefaultKeywordRules {

    private DefaultKeywordRules() {
    }

    static final Map<String, List<String>> CATEGORY = Map.of(
            "HARDWARE", List.of("laptop", "desktop", "monitor", "keyboard", "mouse", "projector", "battery", "screen",
                    "hardware", "power supply", "ups", "access point lights", "not booting", "black screen"),
            "SOFTWARE", List.of("install", "licence", "license", "crash", "update", "software", "office", "antivirus",
                    "matlab", "windows update", "packages", "print spooler"),
            "NETWORK", List.of("wifi", "wi fi", "internet", "lan", "ethernet", "network", "router", "slow connection",
                    "packet loss", "vpn", "dns", "dhcp", "ip address", "disconnects"),
            "DATABASE", List.of("database", "query", "sql", "deadlock", "replication", "db timeout", "connection pool"),
            "CLOUD", List.of("cloud", "vm", "virtual machine", "instance", "bucket", "sync client"),
            "SECURITY", List.of("phishing", "malware", "virus", "breach", "suspicious", "hacked", "ransomware", "spam",
                    "fake email", "fake page", "others grades"),
            "AUTHENTICATION", List.of("login", "log in", "password", "sso", "otp", "locked out", "sign in", "2fa",
                    "credentials", "two factor", "cannot sign in"),
            "APPLICATION", List.of("portal", "app", "page not loading", "error 500", "erp", "lms", "moodle", "payment",
                    "checkout", "transaction", "fee", "website", "upload"),
            "STORAGE", List.of("disk", "quota", "file share", "backup", "nas", "storage full", "mailbox full",
                    "no space", "out of space"));

    static final Map<String, List<String>> SERVICE = Map.ofEntries(
            Map.entry("Fee Payment App", List.of("fee portal", "fee payment", "pay fees", "fee app", "upi",
                    "fee receipt", "semester fee", "payment page")),
            Map.entry("Payment Gateway API", List.of("payment gateway", "merchant", "charged twice", "gateway timeout")),
            Map.entry("Campus SSO", List.of("sso", "single sign on", "campus login", "otp")),
            Map.entry("LDAP Directory", List.of("ldap", "directory", "account locked", "provisioning")),
            Map.entry("Core Campus Network", List.of("campus network", "internet outage", "vpn", "isp", "core switch")),
            Map.entry("Data-Centre Power", List.of("ups", "data centre", "data center", "power cut")),
            Map.entry("DNS/DHCP", List.of("dns", "dhcp", "ip address", "not resolving")),
            Map.entry("Database Cluster", List.of("database", "replica", "deadlock", "db")),
            Map.entry("LMS", List.of("lms", "moodle", "course page", "assignment upload", "quiz", "lecture video")),
            Map.entry("ERP Portal", List.of("erp", "attendance", "timetable", "grade report", "grades")),
            Map.entry("Email & Collaboration", List.of("email", "e mail", "mail", "outlook", "mailbox", "calendar")),
            Map.entry("Hostel Wi-Fi", List.of("hostel wifi", "hostel wi fi", "hostel", "block 5", "block 2")),
            Map.entry("Academic Wi-Fi", List.of("lecture hall wifi", "academic wifi", "classroom wifi", "reading hall")),
            Map.entry("Library Portal", List.of("library portal", "e journals", "catalogue", "library website")),
            Map.entry("Cloud Storage", List.of("shared drive", "file share", "cloud storage", "sync client", "research vm")),
            Map.entry("Lab Workstations", List.of("lab pc", "lab computer", "lab machines", "cs lab", "lab 3")),
            Map.entry("Print Services", List.of("printer", "print", "printing", "print queue")));

    static List<KeywordRule> all() {
        List<KeywordRule> rules = new ArrayList<>();
        CATEGORY.forEach((cat, phrases) -> phrases.forEach(p -> rules.add(new KeywordRule(null, KeywordRule.Target.CATEGORY, cat, p))));
        SERVICE.forEach((svc, phrases) -> phrases.forEach(p -> rules.add(new KeywordRule(null, KeywordRule.Target.SERVICE, svc, p))));
        return rules;
    }
}
