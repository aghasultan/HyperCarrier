package dev.hypercarrier.patcher.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import dev.hypercarrier.patcher.data.SubscriptionData

/**
 * Helper to query SIM slots, active subscriptions, and eSIM profiles.
 */
class SubscriptionHelper(private val context: Context) {

    companion object {
        private const val TAG = "SubscriptionHelper"
    }

    private val subscriptionManager: SubscriptionManager? =
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    /**
     * Fetches all active subscriptions (SIM 1, SIM 2, eSIM).
     */
    @SuppressLint("MissingPermission")
    fun getActiveSubscriptions(): List<SubscriptionData> {
        val sm = subscriptionManager ?: return emptyList()
        return try {
            val activeList: List<SubscriptionInfo>? = sm.activeSubscriptionInfoList
            if (activeList.isNullOrEmpty()) {
                Log.w(TAG, "No active subscriptions found")
                return emptyList()
            }

            val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()

            activeList.map { info ->
                SubscriptionData(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    carrierName = info.carrierName?.toString() ?: "Unknown Carrier",
                    number = try { info.number ?: "" } catch (t: Throwable) { "" },
                    countryIso = info.countryIso?.uppercase() ?: "",
                    mcc = info.mccString ?: "",
                    mnc = info.mncString ?: "",
                    isEmbedded = info.isEmbedded,
                    isOpportunistic = info.isOpportunistic,
                    isDataActive = info.subscriptionId == defaultDataSubId
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error querying active subscriptions", t)
            emptyList()
        }
    }

    /**
     * Resolves the default data subscription ID.
     */
    fun getDefaultDataSubId(): Int {
        return SubscriptionManager.getDefaultDataSubscriptionId()
    }
}
