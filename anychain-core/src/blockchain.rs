use crate::block::Block;
use crate::error::{Error, Result};
use crate::transaction::Transaction;

pub struct Blockchain {
    tip: String,
    db: sled::Db,
}

impl Blockchain {
    /// Opens (or creates) a blockchain at the given filesystem path.
    pub fn open(path: &str) -> Result<Blockchain> {
        log::info!("Opening blockchain at '{}'", path);
        let db = sled::open(path)?;

        let tip = match db.get("LAST")? {
            Some(hash) => String::from_utf8(hash.to_vec())?,
            None => {
                let genesis = Block::genesis()?;
                let hash = genesis.hash().to_string();
                db.insert(&hash, bincode::serialize(&genesis)?)?;
                db.insert("LAST", hash.as_bytes())?;
                db.flush()?;
                log::info!("Created genesis block: {}", hash);
                hash
            }
        };

        Ok(Blockchain { tip, db })
    }

    /// Mines a new block containing a single transaction with `data` and appends it to the chain.
    pub fn add_block(&mut self, data: impl Into<String>) -> Result<Block> {
        let prev_hash = self.tip.clone();
        let height = self.height();
        let tx = Transaction::new(data);
        let block = Block::new(vec![tx], prev_hash, height)?;

        self.db.insert(block.hash(), bincode::serialize(&block)?)?;
        self.db.insert("LAST", block.hash().as_bytes())?;
        self.db.flush()?;
        self.tip = block.hash().to_string();

        Ok(block)
    }

    /// Returns a block by its hash, or `None` if not found.
    pub fn get_block(&self, hash: &str) -> Result<Option<Block>> {
        match self.db.get(hash)? {
            Some(data) => Ok(Some(bincode::deserialize(&data)?)),
            None => Ok(None),
        }
    }

    /// Returns all blocks from tip to genesis.
    pub fn blocks(&self) -> Vec<Block> {
        self.iter().collect()
    }

    /// Validates every block in the chain.
    pub fn is_valid(&self) -> bool {
        let blocks: Vec<Block> = self.blocks();
        for (i, block) in blocks.iter().enumerate() {
            if !block.is_valid() {
                return false;
            }
            if i + 1 < blocks.len() && block.previous_hash != blocks[i + 1].hash {
                return false;
            }
        }
        true
    }

    pub fn tip(&self) -> &str {
        &self.tip
    }

    pub fn height(&self) -> u64 {
        self.iter().count() as u64
    }

    pub fn iter(&self) -> BlockchainIter<'_> {
        BlockchainIter { current: self.tip.clone(), bc: self }
    }
}

pub struct BlockchainIter<'a> {
    current: String,
    bc: &'a Blockchain,
}

impl<'a> Iterator for BlockchainIter<'a> {
    type Item = Block;

    fn next(&mut self) -> Option<Self::Item> {
        if self.current.is_empty() {
            return None;
        }
        let data = self.bc.db.get(self.current.as_bytes()).ok()??;
        let block: Block = bincode::deserialize(&data).ok()?;
        self.current = block.previous_hash.clone();
        Some(block)
    }
}

impl From<Error> for String {
    fn from(e: Error) -> Self {
        e.to_string()
    }
}
