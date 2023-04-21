package com.salesforce.emp.connector.example

import java.util.function.Function
import java.util.function.Supplier

/**
 * Container for BayeuxParameters and the bearerToken.
 * Calls BayeuxParameters supplier in re-authentication scenarios.
 *
 * @author pbn-sfdc
 * modified by bjorn.hagglund
 */
class BearerTokenProviderVariant(private val sessionSupplier: Supplier<BayeuxParametersVariant>) :
    Function<Boolean, String> {
    private var bearerToken: String? = null

    fun login(): BayeuxParametersVariant {
        val parameters = sessionSupplier.get()
        bearerToken = parameters.bearerToken()
        return parameters
    }

    override fun apply(reAuth: Boolean): String {
        if (reAuth) {
            bearerToken = try {
                sessionSupplier.get().bearerToken()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return bearerToken!!
    }
}
