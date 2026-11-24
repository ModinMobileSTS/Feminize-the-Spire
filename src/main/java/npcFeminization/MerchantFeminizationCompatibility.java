package npcFeminization;

import com.evacipated.cardcrawl.modthespire.Loader;

public final class MerchantFeminizationCompatibility {
    private MerchantFeminizationCompatibility() {
    }

    public static boolean isActive() {
        return Loader.isModLoaded("MerchantFeminization");
    }
}
