package net.yapbam.android.transaction;

import net.yapbam.data.Transaction;

public interface TransactionDateDisplayer {
	/** Gets the integer representation of the transaction's date.
	 * <br>The implementor is free to return transaction date or transaction value date depending on its needs.
	 * @param transaction The transaction
	 * @return The integer representation of the transaction's date or null to not display the date.
	 */
	public Integer getDisplayedDate(Transaction transaction);

	/** Gets the layout id.
	 * @return an int, id of the layout resource.
	 */
	public int getLayoutResource();

	/** Gets the layout background color.
	 * @param position The item position
	 * @return a int, id of the color resource.
	 */
	public int getBackgroundColorResource(int position);
}
