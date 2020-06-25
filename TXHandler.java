import java.util.ArrayList;
public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */


    public TxHandler(UTXOPool utxoPool)
    {
        this._utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    private UTXOPool _utxoPool;

    public boolean isValidTx(Transaction tx)
    {
        double sumIn = 0; // sets the input sum to 0
        double sumOut = 0; // sets the output sum to 0
        ArrayList<UTXO> usedUTXO = new ArrayList<>();

        for (int i=0; i<tx.numInputs(); i++) // for all transaactions
        {
            Transaction.Input input = tx.getInput(i); // get hte input

            int outputIndex = input.outputIndex; //

            byte[] prevTxHash = input.prevTxHash; // set previous hashcode to the new input
            byte[] signature = input.signature; // gets signature from key

            UTXO utxo = new UTXO(prevTxHash, outputIndex); // new block

            if (sumIn < sumOut) return false; // if the sum inout is less than sum of output then return false


            Transaction.Output output = _utxoPool.getTxOutput(utxo);

            byte[] message = tx.getRawDataToSign(i);

            if (!Crypto.verifySignature(output.address,message,signature)) return false; //check to see if signatures are valid


            if (usedUTXO.contains(utxo)) return false; // if the utxo is found more than once

            usedUTXO.add(utxo);
            sumIn += output.value;
        }

        for (int i=0;i<tx.numOutputs();i++) // make sure all transactions are not negative
        {
            Transaction.Output output = tx.getOutput(i);
            if (output.value < 0) return false;

            sumOut += output.value;
        }

        return true; // return true if passes all tests
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs)
    {
        ArrayList<Transaction> validTxs = new ArrayList<>();

        for (Transaction tx : possibleTxs)
        {
            if (isValidTx(tx)) // if its a valid transaction
            {
                validTxs.add(tx); // add transaction

                // when valid transaction occur this will remove the old utxo to be replaced by the new one from the updated transaction
                for (Transaction.Input input : tx.getInputs())
                {
                    int outputIndex = input.outputIndex;
                    byte[] prevTxHash = input.prevTxHash;
                    UTXO utxo = new UTXO(prevTxHash, outputIndex);
                    _utxoPool.removeUTXO(utxo);

                }
                // adds the new transaction to the utxo
                byte[] hash = tx.getHash();

                for (int i=0;i<tx.numOutputs();i++) // for the outputs gathered add to the utxo
                {
                    UTXO utxo = new UTXO(hash, i);
                    _utxoPool.addUTXO(utxo, tx.getOutput(i));
                }
            }
        }
        Transaction[] validTxsArr = new Transaction[validTxs.size()]; //creates new valid transaction array

        validTxsArr = validTxs.toArray(validTxsArr); // adds transactiob to the array

        return validTxsArr; // returns the transaction array
    }

}
