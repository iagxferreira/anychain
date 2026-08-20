package dev.anychain.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.registerCloseable
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.anychain.core.Blockchain
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration

class Anychain : CliktCommand(name = "anychain") {
    override fun help(context: Context) = "A proof-of-concept blockchain"

    /** Path to the blockchain database directory. */
    private val db by option("--db", envvar = "ANYCHAIN_DB")
        .default("/tmp/anychain")

    init {
        versionOption("0.1.0")
        subcommands(AddCommand(), PrintCommand(), ValidateCommand())
    }

    override fun run() {
        currentContext.obj = currentContext.registerCloseable(Blockchain.open(db))
    }
}

private abstract class DbSubcommand(name: String) : CliktCommand(name = name) {
    protected val chain: Blockchain by requireObject()
}

/** Mine a new block with the given data. */
private class AddCommand : DbSubcommand("add") {
    override fun help(context: Context) = "Mine a new block with the given data"

    /** Data to store in the block. */
    private val data by argument()

    override fun run() {
        echo("Mining block...")
        val block = chain.addBlock(data)
        echo("Block added!")
        echo("  Hash   : ${block.hash}")
        echo("  Height : ${block.height}")
        echo("  Nonce  : ${block.nonce}")
    }
}

/** Print all blocks from tip to genesis. */
private class PrintCommand : DbSubcommand("print") {
    override fun help(context: Context) = "Print all blocks from tip to genesis"

    override fun run() {
        val blocks = chain.blocks()
        if (blocks.isEmpty()) {
            echo("Chain is empty.")
        }
        for (block in blocks) {
            echo("Height      : ${block.height}")
            echo("Hash        : ${block.hash}")
            echo("Prev hash   : ${block.previousHash}")
            echo("Nonce       : ${block.nonce}")
            echo("Timestamp   : ${block.timestamp}")
            echo("Transactions:")
            for (tx in block.transactions) {
                echo("  [${tx.id}] ${tx.data}")
            }
            echo("-".repeat(60))
        }
    }
}

/** Validate the integrity of the chain. */
private class ValidateCommand : DbSubcommand("validate") {
    override fun help(context: Context) = "Validate the integrity of the chain"

    override fun run() {
        if (chain.isValid()) {
            echo("Chain is valid.")
        } else {
            echo("Chain is INVALID.", err = true)
            throw ProgramResult(1)
        }
    }
}

fun main(args: Array<String>) {
    KotlinLoggingConfiguration.logStartupMessage = false
    Anychain().main(args)
}
