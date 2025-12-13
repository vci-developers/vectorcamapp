package com.vci.vectorcamapp.intake.domain.model

object IntakeDropdownOptions {

    enum class CollectionMethodOption(val label: String) {
        CDC_LIGHT_TRAP("CDC Light Trap (LTC)"),
        PYRETHRUM_SPRAY_CATCH("Pyrethrum Spray Catch (PSC)"),
        HUMAN_LANDING_CATCH("Human Landing Catch (HLC)"),
        OTHER("Other")
    }

    enum class SpecimenConditionOption(val label: String) {
        FRESH("Fresh"),
        DESSICATED("Dessicated"),
        OTHER("Other")
    }

    enum class LlinTypeOption(val label: String) {
        PYRETHROID_ONLY("Pyrethroid Only"),
        PYRETHROID_PBO("Pyrethroid + PBO"),
        PYRETHROID_CHLORFENAPYR("Pyrethroid + Chlorfenapyr"),
        PYRETHROID_PYRIPROXYFEN("Pyrethroid + Pyriproxyfen"),
        OTHER("Other")
    }

    enum class LlinBrandOption(
        val label: String,
        val type: LlinTypeOption? = null
    ) {
        OLYSET_NET("OLYSET Net", LlinTypeOption.PYRETHROID_ONLY),
        INTERCEPTOR("Interceptor", LlinTypeOption.PYRETHROID_ONLY),
        ROYAL_SENTRY("Royal Sentry", LlinTypeOption.PYRETHROID_ONLY),
        ROYAL_SENTRY_2("Royal Sentry 2.0", LlinTypeOption.PYRETHROID_ONLY),
        PERMANET_2("PermaNet 2.0", LlinTypeOption.PYRETHROID_ONLY),
        DURANET_LLIN("Duranet LLIN", LlinTypeOption.PYRETHROID_ONLY),
        MIRANET("MiraNet", LlinTypeOption.PYRETHROID_ONLY),
        MAGNET("MAGNet", LlinTypeOption.PYRETHROID_ONLY),
        YAHE_LN("Yahe LN", LlinTypeOption.PYRETHROID_ONLY),
        SAFENET("SafeNet", LlinTypeOption.PYRETHROID_ONLY),
        YORKOOL_LN("Yorkool LN", LlinTypeOption.PYRETHROID_ONLY),
        PANDA_NET_2("Panda Net 2.0", LlinTypeOption.PYRETHROID_ONLY),
        TSARA_SOFT("Tsara Soft", LlinTypeOption.PYRETHROID_ONLY),
        OLYSET_PLUS("OLYSET Plus", LlinTypeOption.PYRETHROID_PBO),
        PERMANET_3("PermaNet 3.0", LlinTypeOption.PYRETHROID_PBO),
        VEERALIN("VEERALIN", LlinTypeOption.PYRETHROID_PBO),
        TSARA_BOOST("Tsara Boost", LlinTypeOption.PYRETHROID_PBO),
        TSARA_PLUS("Tsara Plus", LlinTypeOption.PYRETHROID_PBO),
        INTERCEPTOR_G2("Interceptor G2", LlinTypeOption.PYRETHROID_CHLORFENAPYR),
        ROYAL_GUARD("Royal Guard", LlinTypeOption.PYRETHROID_PYRIPROXYFEN),
        OTHER("Other", null)
    }
}
