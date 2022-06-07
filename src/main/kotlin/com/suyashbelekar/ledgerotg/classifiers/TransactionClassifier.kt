package com.suyashbelekar.ledgerotg.classifiers

import com.suyashbelekar.ledgerotg.ledger.RegisterRow
import jakarta.inject.Singleton
import weka.classifiers.bayes.NaiveBayesUpdateable
import weka.core.*
import java.io.File
import java.io.Serializable

interface TransactionClassifier {
    fun classify(classifiableTransaction: ClassifiableTransaction): String?
}

data class ClassifiableTransaction(
    val amount: Double,
    val payee: String?
)

const val modelFileName = "modal.data"

@Singleton
class WekaTransactionClassifier(
    private val dataDir: File
) : TransactionClassifier {
    private val persistentModelData by lazy { loadTrainedModel() }

    override fun classify(classifiableTransaction: ClassifiableTransaction): String? {
        val modelData = persistentModelData ?: throw ModelNotTrainedException()

        val payeeNominal = modelData.payeeMap[classifiableTransaction.payee]
            ?: modelData.payeeMap["Unknown Payee"]!!

        val instance = DenseInstance(5).apply {
            setDataset(modelData.structure)
            setValue(1, classifiableTransaction.amount)
            setValue(2, payeeNominal)
        }

        val prediction = modelData.classifier.classifyInstance(instance)

        if (prediction == Utils.missingValue()) {
            return null
        }

        return modelData.structure.classAttribute().value(prediction.toInt())
    }

    fun retrain(registerRows: Sequence<RegisterRow>, distinctAccountNames: Set<String>) {
        if (distinctAccountNames.isEmpty()) {
            return
        }

        val nonUnaryClasses: Set<String> = if (distinctAccountNames.size == 1) {
            distinctAccountNames + ""
        } else {
            distinctAccountNames
        }

        val attributes = arrayListOf(
            Attribute("@@class@@", nonUnaryClasses.toList()),
            Attribute("amount"),
            Attribute("payee")
        )

        val structure = Instances("TransactionInstances", attributes, 0).apply {
            setClassIndex(0)
        }

        val classifier = NaiveBayesUpdateable().apply {
            buildClassifier(structure)
        }

        val payeeMap = mutableMapOf("Unknown Payee" to 0.0)

        registerRows.forEach { transaction ->
            val nominal = payeeMap[transaction.payee] ?: run {
                val nominal = payeeMap.size.toDouble()
                payeeMap[transaction.payee] = nominal
                nominal
            }

            val instance = DenseInstance(5).apply {
                setDataset(structure)
                setValue(0, transaction.account.fullName)
                setValue(1, transaction.amount.value)
                setValue(2, nominal)
            }

            classifier.updateClassifier(instance)
        }

        saveTrainedModel(structure, payeeMap, classifier)
    }

    private fun saveTrainedModel(
        structure: Instances,
        payeeMap: MutableMap<String, Double>,
        classifier: NaiveBayesUpdateable
    ) {
        File(dataDir, modelFileName).outputStream().buffered().use {
            SerializationHelper.write(
                it,
                PersistentModelData(structure, payeeMap, classifier)
            )
            it.flush()
        }
    }

    private fun loadTrainedModel(): PersistentModelData? {
        return try {
            File(dataDir, modelFileName).inputStream().buffered().use {
                SerializationHelper.read(it) as PersistentModelData
            }
        } catch (e: Exception) {
            null
        }
    }

    data class PersistentModelData(
        val structure: Instances,
        val payeeMap: Map<String, Double>,
        val classifier: NaiveBayesUpdateable
    ) : Serializable
}

class ModelNotTrainedException : IllegalStateException()