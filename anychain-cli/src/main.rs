use anyhow::Result;
use clap::{Parser, Subcommand};

use anychain_core::Blockchain;

#[derive(Parser)]
#[command(
    name = "anychain",
    version,
    about = "A proof-of-concept blockchain",
    long_about = None
)]
struct Cli {
    /// Path to the blockchain database directory
    #[arg(long, env = "ANYCHAIN_DB", default_value = "/tmp/anychain")]
    db: String,

    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    /// Mine a new block with the given data
    Add {
        /// Data to store in the block
        data: String,
    },
    /// Print all blocks from tip to genesis
    Print,
    /// Validate the integrity of the chain
    Validate,
}

fn main() -> Result<()> {
    env_logger::init();

    let cli = Cli::parse();
    let mut bc = Blockchain::open(&cli.db)?;

    match cli.command {
        Commands::Add { data } => {
            println!("Mining block...");
            let block = bc.add_block(data)?;
            println!("Block added!");
            println!("  Hash   : {}", block.hash());
            println!("  Height : {}", block.height);
            println!("  Nonce  : {}", block.nonce);
        }
        Commands::Print => {
            let blocks = bc.blocks();
            if blocks.is_empty() {
                println!("Chain is empty.");
            }
            for block in blocks {
                println!("Height      : {}", block.height);
                println!("Hash        : {}", block.hash());
                println!("Prev hash   : {}", block.previous_hash);
                println!("Nonce       : {}", block.nonce);
                println!("Timestamp   : {}", block.timestamp);
                println!("Transactions:");
                for tx in &block.transactions {
                    println!("  [{}] {}", tx.id, tx.data);
                }
                println!("{}", "-".repeat(60));
            }
        }
        Commands::Validate => {
            if bc.is_valid() {
                println!("Chain is valid.");
            } else {
                eprintln!("Chain is INVALID.");
                std::process::exit(1);
            }
        }
    }

    Ok(())
}
