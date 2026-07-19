package com.sami.app.demo;

import java.util.List;

/**
 * Static, realistic source data for the demo generator — Iranian people and
 * places, and a mobile / accessory / gaming / digital product catalogue that
 * mirrors what an Iranian phone retailer actually stocks. No Lorem Ipsum: every
 * value is a plausible real-world one, expanded into volume by the seeder via
 * storage/colour variants.
 */
final class DemoDataPools {

    private DemoDataPools() {
    }

    // ---- People -------------------------------------------------------------

    static final String[] FIRST_NAMES_MALE = {
            "علی", "محمد", "رضا", "حسین", "مهدی", "امیر", "سعید", "حسن", "احمد", "مجید",
            "کاظم", "بهروز", "فرهاد", "کیان", "آرش", "بابک", "پویا", "سینا", "میلاد", "نیما",
            "امین", "وحید", "یاسر", "پارسا", "سام", "شایان", "کامران", "مسعود", "بهنام", "ابراهیم"
    };

    static final String[] FIRST_NAMES_FEMALE = {
            "فاطمه", "زهرا", "مریم", "نرگس", "سارا", "لیلا", "مینا", "الهام", "نازنین", "شقایق",
            "سمیرا", "پریسا", "مهسا", "نگار", "یاسمن", "هانیه", "الناز", "رویا", "شیوا", "آیدا",
            "کیمیا", "ملیکا", "درسا", "ستایش", "نیلوفر", "بهاره", "فرزانه", "مهناز", "طاهره", "زینب"
    };

    static final String[] LAST_NAMES = {
            "محمدی", "حسینی", "احمدی", "رضایی", "موسوی", "کریمی", "جعفری", "قاسمی", "کاظمی", "نجفی",
            "صادقی", "رحیمی", "علوی", "طاهری", "اکبری", "بهرامی", "یوسفی", "نوری", "شریفی", "زارع",
            "حیدری", "سلطانی", "غفاری", "مرادی", "فتحی", "عباسی", "امینی", "رستمی", "کوهی", "فرهادی",
            "مقدم", "دهقان", "اسدی", "بابایی", "خانی", "صالحی", "قربانی", "میرزایی", "پورمحمد", "شفیعی"
    };

    static final String[] OCCUPATIONS = {
            "کارمند", "مهندس", "پزشک", "معلم", "کاسب", "دانشجو", "برنامه‌نویس", "حسابدار",
            "فروشنده", "راننده", "وکیل", "پرستار", "طراح", "مدیر فروش", "بازاریاب", "آزاد"
    };

    /** Iranian cities paired with their province. */
    static final List<City> CITIES = List.of(
            new City("تهران", "تهران"),
            new City("مشهد", "خراسان رضوی"),
            new City("اصفهان", "اصفهان"),
            new City("شیراز", "فارس"),
            new City("تبریز", "آذربایجان شرقی"),
            new City("کرج", "البرز"),
            new City("اهواز", "خوزستان"),
            new City("قم", "قم"),
            new City("رشت", "گیلان"),
            new City("یزد", "یزد"),
            new City("کرمان", "کرمان"),
            new City("همدان", "همدان"),
            new City("ارومیه", "آذربایجان غربی"),
            new City("زنجان", "زنجان"),
            new City("ساری", "مازندران"),
            new City("بندرعباس", "هرمزگان"),
            new City("قزوین", "قزوین"),
            new City("اردبیل", "اردبیل")
    );

    static final String[] STREET_NAMES = {
            "خیابان ولیعصر", "خیابان انقلاب", "بلوار فردوسی", "خیابان امام خمینی", "میدان آزادی",
            "خیابان شریعتی", "بلوار کشاورز", "خیابان طالقانی", "خیابان مطهری", "بلوار وکیل‌آباد",
            "خیابان سعدی", "خیابان حافظ", "بلوار معلم", "خیابان جمهوری", "میدان ونک"
    };

    // ---- Suppliers ----------------------------------------------------------

    /** name, category-key, and city index seed. */
    static final List<SupplierTemplate> SUPPLIERS = List.of(
            new SupplierTemplate("پخش موبایل ایرانیان", "APPLE", "واردکننده رسمی"),
            new SupplierTemplate("تجارت الکترونیک پارس", "SAMSUNG", "واردکننده رسمی"),
            new SupplierTemplate("گروه بازرگانی آسیا تل", "APPLE", "عمده‌فروش"),
            new SupplierTemplate("پخش سراسری هوشمند", "XIAOMI", "واردکننده رسمی"),
            new SupplierTemplate("بازرگانی دیجی‌کالای شرق", "ACCESSORY", "توزیع‌کننده لوازم جانبی"),
            new SupplierTemplate("پارس گیم تجارت", "GAMING", "توزیع‌کننده گیمینگ"),
            new SupplierTemplate("موبایل سنتر تهران", "SAMSUNG", "عمده‌فروش"),
            new SupplierTemplate("واردات فناوری نوین", "XIAOMI", "واردکننده رسمی"),
            new SupplierTemplate("پخش لوازم جانبی ماد", "ACCESSORY", "توزیع‌کننده لوازم جانبی"),
            new SupplierTemplate("تجارت گستر کیان", "APPLE", "عمده‌فروش"),
            new SupplierTemplate("بازرگانی هزاره سوم", "DIGITAL", "توزیع‌کننده محصولات دیجیتال"),
            new SupplierTemplate("پخش موبایل البرز", "SAMSUNG", "عمده‌فروش"),
            new SupplierTemplate("فناوران ارتباط جنوب", "XIAOMI", "عمده‌فروش"),
            new SupplierTemplate("گروه صنعتی تک‌استار", "ACCESSORY", "توزیع‌کننده لوازم جانبی"),
            new SupplierTemplate("پلی‌استیشن پارسه", "GAMING", "توزیع‌کننده گیمینگ"),
            new SupplierTemplate("موبایل مارکت اصفهان", "APPLE", "عمده‌فروش")
    );

    static final String[] SUPPLIER_CATEGORIES = {
            "واردکننده رسمی", "عمده‌فروش", "توزیع‌کننده لوازم جانبی", "توزیع‌کننده گیمینگ",
            "توزیع‌کننده محصولات دیجیتال", "پخش سراسری"
    };

    static final String[] SUPPLIER_TAGS = {
            "معتبر", "پرداخت اعتباری", "تسویه نقدی", "ارسال سریع", "قیمت رقابتی", "همکار قدیمی"
    };

    static final String[] TAG_COLORS = {
            "#26375F", "#5A6B8C", "#B0894F", "#2E7D5B", "#9C4A63", "#7A5C9E", "#B5651D"
    };

    static final String[] PAYMENT_METHOD_PREF = {"phone", "email", "whatsapp"};

    // ---- Products -----------------------------------------------------------

    /**
     * A base product. {@code storages} and {@code colors} are expanded into
     * concrete variants; an empty {@code storages} means a single-config item.
     * {@code basePrice} is the retail sale price in Toman for the base config;
     * larger storage tiers add a premium.
     */
    record ProductTemplate(
            String brand,
            String model,
            String category,
            long basePrice,
            boolean imei,
            boolean serial,
            String[] storages,
            String[] colors,
            String specs
    ) {
    }

    record City(String name, String province) {
    }

    record SupplierTemplate(String name, String brandKey, String category) {
    }

    private static final String[] NO_STORAGE = {};

    static final List<ProductTemplate> PRODUCTS = List.of(
            // --- Apple iPhone ---
            new ProductTemplate("Apple", "iPhone 16 Pro Max", "گوشی موبایل", 95_000_000, true, false,
                    new String[]{"256GB", "512GB", "1TB"},
                    new String[]{"مشکی تیتانیوم", "طبیعی تیتانیوم", "سفید تیتانیوم", "صحرایی"},
                    "نمایشگر 6.9 اینچ Super Retina XDR، تراشه A18 Pro، دوربین 48 مگاپیکسل، باتری تمام‌روز"),
            new ProductTemplate("Apple", "iPhone 16 Pro", "گوشی موبایل", 82_000_000, true, false,
                    new String[]{"128GB", "256GB", "512GB"},
                    new String[]{"مشکی تیتانیوم", "طبیعی تیتانیوم", "سفید تیتانیوم"},
                    "نمایشگر 6.3 اینچ، تراشه A18 Pro، سیستم دوربین سه‌گانه حرفه‌ای"),
            new ProductTemplate("Apple", "iPhone 16", "گوشی موبایل", 62_000_000, true, false,
                    new String[]{"128GB", "256GB", "512GB"},
                    new String[]{"مشکی", "آبی روشن", "سبز چای", "صورتی", "فرا بنفش"},
                    "نمایشگر 6.1 اینچ، تراشه A18، دوربین دوگانه 48 مگاپیکسل"),
            new ProductTemplate("Apple", "iPhone 15", "گوشی موبایل", 48_000_000, true, false,
                    new String[]{"128GB", "256GB"},
                    new String[]{"مشکی", "آبی", "سبز", "زرد", "صورتی"},
                    "نمایشگر 6.1 اینچ، تراشه A16 Bionic، پورت USB-C"),
            // --- Samsung ---
            new ProductTemplate("Samsung", "Galaxy S25 Ultra", "گوشی موبایل", 88_000_000, true, false,
                    new String[]{"256GB", "512GB", "1TB"},
                    new String[]{"مشکی تیتانیوم", "خاکستری", "نقره‌ای", "آبی"},
                    "نمایشگر 6.9 اینچ Dynamic AMOLED، اسنپدراگون 8 Elite، قلم S Pen، دوربین 200 مگاپیکسل"),
            new ProductTemplate("Samsung", "Galaxy S25+", "گوشی موبایل", 66_000_000, true, false,
                    new String[]{"256GB", "512GB"},
                    new String[]{"مشکی", "نقره‌ای", "آبی", "سبز"},
                    "نمایشگر 6.7 اینچ، اسنپدراگون 8 Elite، باتری 4900 میلی‌آمپر"),
            new ProductTemplate("Samsung", "Galaxy A56", "گوشی موبایل", 24_000_000, true, false,
                    new String[]{"128GB", "256GB"},
                    new String[]{"مشکی", "طوسی", "صورتی", "زیتونی"},
                    "نمایشگر 6.7 اینچ Super AMOLED، Exynos 1580، دوربین 50 مگاپیکسل"),
            new ProductTemplate("Samsung", "Galaxy A36", "گوشی موبایل", 18_500_000, true, false,
                    new String[]{"128GB", "256GB"},
                    new String[]{"مشکی", "سفید", "لیمویی"},
                    "نمایشگر 6.7 اینچ، اسنپدراگون 6 Gen 3، باتری 5000 میلی‌آمپر"),
            // --- Xiaomi family ---
            new ProductTemplate("Xiaomi", "Xiaomi 15 Ultra", "گوشی موبایل", 71_000_000, true, false,
                    new String[]{"256GB", "512GB", "1TB"},
                    new String[]{"مشکی", "سفید", "نقره‌ای"},
                    "دوربین Leica چهارگانه، اسنپدراگون 8 Elite، نمایشگر 6.73 اینچ 2K"),
            new ProductTemplate("Xiaomi", "Redmi Note 15 Pro", "گوشی موبایل", 16_000_000, true, false,
                    new String[]{"128GB", "256GB", "512GB"},
                    new String[]{"مشکی", "آبی", "بنفش"},
                    "نمایشگر 6.67 اینچ AMOLED 120Hz، دوربین 200 مگاپیکسل، باتری 5500 میلی‌آمپر"),
            new ProductTemplate("Poco", "Poco X7 Pro", "گوشی موبایل", 19_500_000, true, false,
                    new String[]{"256GB", "512GB"},
                    new String[]{"مشکی", "زرد", "سبز"},
                    "دایمنسیتی 8400 Ultra، نمایشگر 6.67 اینچ 120Hz، شارژ 90 وات"),
            new ProductTemplate("Nothing", "Nothing Phone (3)", "گوشی موبایل", 34_000_000, true, false,
                    new String[]{"256GB", "512GB"},
                    new String[]{"مشکی", "سفید"},
                    "طراحی شفاف Glyph، اسنپدراگون سری 8s، نمایشگر 6.7 اینچ AMOLED"),
            new ProductTemplate("Google", "Pixel 10 Pro", "گوشی موبایل", 58_000_000, true, false,
                    new String[]{"128GB", "256GB", "512GB"},
                    new String[]{"مشکی اوبسیدین", "چینی", "سبز"},
                    "تراشه Tensor G5، دوربین محاسباتی گوگل، نمایشگر 6.7 اینچ LTPO"),
            new ProductTemplate("OnePlus", "OnePlus 14", "گوشی موبایل", 52_000_000, true, false,
                    new String[]{"256GB", "512GB"},
                    new String[]{"مشکی", "سبز", "نقره‌ای"},
                    "اسنپدراگون 8 Elite، شارژ 100 وات، نمایشگر 6.82 اینچ 2K"),
            new ProductTemplate("Honor", "Honor Magic 7 Pro", "گوشی موبایل", 49_000_000, true, false,
                    new String[]{"256GB", "512GB"},
                    new String[]{"مشکی", "طلایی"},
                    "دوربین پریسکوپ 200 مگاپیکسل، اسنپدراگون 8 Elite، باتری سیلیکون-کربنی"),
            new ProductTemplate("Motorola", "Moto G85", "گوشی موبایل", 12_500_000, true, false,
                    new String[]{"128GB", "256GB"},
                    new String[]{"مشکی", "سبز", "بنفش"},
                    "نمایشگر pOLED 6.67 اینچ، اسنپدراگون 6s Gen 3"),
            new ProductTemplate("Nokia", "Nokia G60", "گوشی موبایل", 9_500_000, true, false,
                    new String[]{"128GB"},
                    new String[]{"مشکی", "آبی"},
                    "بدنه مقاوم، سه سال آپدیت اندروید، باتری 4500 میلی‌آمپر"),
            // --- Tablets ---
            new ProductTemplate("Apple", "iPad Pro 13 M4", "تبلت", 84_000_000, false, true,
                    new String[]{"256GB", "512GB", "1TB"},
                    new String[]{"نقره‌ای", "خاکستری"},
                    "نمایشگر Ultra Retina XDR، تراشه M4، پشتیبانی Apple Pencil Pro"),
            new ProductTemplate("Samsung", "Galaxy Tab S10 Ultra", "تبلت", 62_000_000, false, true,
                    new String[]{"256GB", "512GB"},
                    new String[]{"خاکستری", "نقره‌ای"},
                    "نمایشگر 14.6 اینچ AMOLED، قلم S Pen همراه، دایمنسیتی 9300+"),
            new ProductTemplate("Xiaomi", "Xiaomi Pad 7 Pro", "تبلت", 21_000_000, false, true,
                    new String[]{"128GB", "256GB"},
                    new String[]{"مشکی", "سبز", "آبی"},
                    "نمایشگر 11.2 اینچ 3.2K، اسنپدراگون 8s Gen 3"),
            // --- Smart watches ---
            new ProductTemplate("Apple", "Apple Watch Ultra 2", "ساعت هوشمند", 41_000_000, false, true,
                    NO_STORAGE,
                    new String[]{"تیتانیوم مشکی", "تیتانیوم طبیعی"},
                    "بدنه تیتانیوم 49 میلی‌متری، GPS دوباند، مقاومت آب تا 100 متر"),
            new ProductTemplate("Apple", "Apple Watch Series 10", "ساعت هوشمند", 23_000_000, false, true,
                    new String[]{"42mm", "46mm"},
                    new String[]{"مشکی", "نقره‌ای", "طلایی رز"},
                    "نمایشگر بزرگ‌تر و نازک‌تر، سنسور دمای بدن، حسگر اکسیژن خون"),
            new ProductTemplate("Samsung", "Galaxy Watch Ultra", "ساعت هوشمند", 26_000_000, false, true,
                    new String[]{"47mm"},
                    new String[]{"تیتانیوم خاکستری", "تیتانیوم سفید"},
                    "بدنه تیتانیوم، دکمه Quick، باتری 590 میلی‌آمپر"),
            new ProductTemplate("Xiaomi", "Xiaomi Smart Band 9", "دستبند سلامتی", 2_200_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "نارنجی", "صورتی"},
                    "نمایشگر AMOLED، عمر باتری تا 21 روز، پایش ضربان و اکسیژن"),
            // --- Audio accessories ---
            new ProductTemplate("Apple", "AirPods Pro 2", "هندزفری", 9_800_000, false, true,
                    NO_STORAGE,
                    new String[]{"سفید"},
                    "حذف نویز فعال، صدای فضایی، کیس شارژ USB-C"),
            new ProductTemplate("Samsung", "Galaxy Buds3 Pro", "هندزفری", 7_400_000, false, true,
                    NO_STORAGE,
                    new String[]{"سفید", "نقره‌ای"},
                    "درایور دوگانه، حذف نویز تطبیقی، صدای 24 بیت Hi-Fi"),
            new ProductTemplate("Anker", "Soundcore Life P3", "هندزفری", 2_600_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "آبی", "صورتی", "سفید"},
                    "حذف نویز، اپلیکیشن اختصاصی، مقاومت IPX5"),
            new ProductTemplate("JBL", "JBL Tune Beam", "هندزفری", 3_100_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "سفید", "آبی"},
                    "صدای JBL Pure Bass، حذف نویز، عمر باتری 48 ساعت"),
            new ProductTemplate("JBL", "JBL Flip 6", "اسپیکر بلوتوث", 5_500_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "آبی", "قرمز", "خاکی"},
                    "خروجی 30 وات، مقاومت IP67، PartyBoost"),
            // --- Power / cables / chargers ---
            new ProductTemplate("Anker", "Anker PowerCore 20000", "پاوربانک", 3_200_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "سفید"},
                    "ظرفیت 20000 میلی‌آمپر، خروجی 30 وات USB-C PD"),
            new ProductTemplate("Xiaomi", "Xiaomi Power Bank 10000", "پاوربانک", 1_450_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "سفید"},
                    "ظرفیت 10000 میلی‌آمپر، شارژ سریع 22.5 وات"),
            new ProductTemplate("Apple", "Apple 20W USB-C Adapter", "شارژر", 1_100_000, false, false,
                    NO_STORAGE,
                    new String[]{"سفید"},
                    "شارژر اصلی 20 وات با پورت USB-C"),
            new ProductTemplate("Samsung", "Samsung 45W Charger", "شارژر", 1_650_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "سفید"},
                    "شارژر فوق سریع 45 وات Super Fast Charging 2.0"),
            new ProductTemplate("Anker", "Anker 622 MagGo Wireless", "شارژر بی‌سیم", 2_900_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "آبی", "سبز"},
                    "شارژر بی‌سیم مغناطیسی با پایه تاشو"),
            new ProductTemplate("Baseus", "Baseus USB-C to USB-C 100W", "کابل شارژ", 650_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "سفید"},
                    "کابل 100 وات با روکش نایلونی و طول 1 متر"),
            new ProductTemplate("Baseus", "Baseus Car Charger 60W", "شارژر فندکی", 980_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "نقره‌ای"},
                    "شارژر فندکی دو پورت با خروجی 60 وات"),
            // --- Protection ---
            new ProductTemplate("Spigen", "Spigen Tough Armor Case", "قاب گوشی", 850_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "خاکستری", "سرمه‌ای"},
                    "قاب ضدضربه با استند، محافظت نظامی درجه یک"),
            new ProductTemplate("Nillkin", "Nillkin Frosted Shield", "قاب گوشی", 480_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "آبی", "قرمز"},
                    "قاب مات مقاوم با گریپ مناسب"),
            new ProductTemplate("Mytemp", "Full Cover Glass", "محافظ صفحه", 320_000, false, false,
                    NO_STORAGE,
                    new String[]{"شفاف", "مات"},
                    "گلس تمام‌صفحه 9H با پوشش کامل لبه"),
            new ProductTemplate("Mytemp", "Camera Lens Protector", "محافظ لنز دوربین", 250_000, false, false,
                    NO_STORAGE,
                    new String[]{"شفاف"},
                    "محافظ فلزی لنز دوربین با شیشه سکوریت"),
            new ProductTemplate("Baseus", "Magnetic Car Holder", "هولدر", 720_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی", "نقره‌ای"},
                    "پایه نگهدارنده مغناطیسی داشبوردی خودرو"),
            // --- Storage ---
            new ProductTemplate("SanDisk", "SanDisk Extreme microSD 128GB", "کارت حافظه", 1_250_000, false, false,
                    NO_STORAGE,
                    new String[]{"قرمز"},
                    "سرعت خواندن 190 مگابایت بر ثانیه، کلاس A2 V30"),
            new ProductTemplate("SanDisk", "SanDisk Ultra Flash 64GB", "فلش مموری", 780_000, false, false,
                    NO_STORAGE,
                    new String[]{"مشکی"},
                    "فلش USB 3.0 با سرعت انتقال بالا"),
            // --- Gaming (serial, not imei) ---
            new ProductTemplate("Sony", "PlayStation 5 Slim", "کنسول بازی", 42_000_000, false, true,
                    new String[]{"Disc", "Digital"},
                    new String[]{"سفید"},
                    "کنسول نسل نهم، SSD فوق سریع، پشتیبانی 4K و Ray Tracing"),
            new ProductTemplate("Sony", "PlayStation 5 Pro", "کنسول بازی", 78_000_000, false, true,
                    NO_STORAGE,
                    new String[]{"سفید"},
                    "GPU قدرتمندتر، آپ‌اسکیل PSSR، حافظه 2 ترابایت"),
            new ProductTemplate("Sony", "PlayStation Portal", "کنسول بازی", 16_500_000, false, true,
                    NO_STORAGE,
                    new String[]{"سفید"},
                    "نمایشگر 8 اینچ برای Remote Play، دسته DualSense داخلی"),
            new ProductTemplate("Microsoft", "Xbox Series X", "کنسول بازی", 39_000_000, false, true,
                    NO_STORAGE,
                    new String[]{"مشکی"},
                    "12 ترافلاپس، 4K/120fps، درایو دیسک، SSD یک ترابایت"),
            new ProductTemplate("Microsoft", "Xbox Series S", "کنسول بازی", 22_000_000, false, true,
                    NO_STORAGE,
                    new String[]{"سفید", "مشکی"},
                    "کنسول دیجیتال جمع‌وجور، 1440p، SSD 512 گیگابایت"),
            new ProductTemplate("Nintendo", "Nintendo Switch OLED", "کنسول بازی", 21_500_000, false, true,
                    NO_STORAGE,
                    new String[]{"سفید", "نئون"},
                    "نمایشگر OLED هفت اینچ، حالت رومیزی و دستی"),
            new ProductTemplate("Sony", "DualSense Controller", "دسته بازی", 4_800_000, false, false,
                    NO_STORAGE,
                    new String[]{"سفید", "مشکی", "قرمز", "بنفش"},
                    "دسته بی‌سیم با بازخورد لمسی و ماشه تطبیقی"),
            // --- Digital products (no imei/serial; stock = code count) ---
            new ProductTemplate("Sony", "گیفت کارت پلی‌استیشن ۵۰ دلاری", "گیفت کارت", 4_200_000, false, false,
                    NO_STORAGE,
                    new String[]{"دیجیتال"},
                    "کد شارژ کیف پول PSN منطقه آمریکا"),
            new ProductTemplate("Apple", "گیفت کارت اپل ۱۰۰ دلاری", "گیفت کارت", 8_100_000, false, false,
                    NO_STORAGE,
                    new String[]{"دیجیتال"},
                    "کد شارژ App Store و iTunes"),
            new ProductTemplate("Steam", "گیفت کارت استیم ۲۰ دلاری", "گیفت کارت", 1_750_000, false, false,
                    NO_STORAGE,
                    new String[]{"دیجیتال"},
                    "کد شارژ کیف پول Steam"),
            new ProductTemplate("Microsoft", "اشتراک Xbox Game Pass Ultimate", "اشتراک دیجیتال", 2_300_000, false, false,
                    NO_STORAGE,
                    new String[]{"سه ماهه"},
                    "دسترسی به کتابخانه بازی‌های Game Pass روی کنسول و PC"),
            new ProductTemplate("Google", "لایسنس اورجینال ویندوز ۱۱ پرو", "لایسنس نرم‌افزار", 3_600_000, false, false,
                    NO_STORAGE,
                    new String[]{"دیجیتال"},
                    "کلید فعال‌سازی اصلی ویندوز ۱۱ نسخه Pro")
    );

    static final String[] PRODUCT_WARRANTIES = {
            "گارانتی ۱۸ ماهه شرکتی", "گارانتی ۱۲ ماهه", "گارانتی ۶ ماهه", "بدون گارانتی (اصل)",
            "گارانتی مادام‌العمر", "گارانتی ۲۴ ماهه"
    };
}
