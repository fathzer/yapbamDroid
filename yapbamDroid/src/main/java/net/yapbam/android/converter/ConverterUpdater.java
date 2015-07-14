package net.yapbam.android.converter;

import net.astesana.android.async.ExtendedAsyncTask;
import net.yapbam.currency.AbstractCurrencyConverter;

import java.io.IOException;
import java.text.ParseException;

final class ConverterUpdater extends ExtendedAsyncTask<Void, Void, Void> {
	private boolean forced;
	private AbstractCurrencyConverter converter;

	public ConverterUpdater(AbstractCurrencyConverter converter, boolean forced) {
		this.converter = converter;
		this.forced = forced;
	}

	@Override
	protected Void exceptionFreeDoInBackground(Void[] args) throws IOException, ParseException {
		if (forced) {
			converter.forcedUpdate();
		} else {
			converter.update();
		}
		return null;
	}
	
	public boolean isForced() {
		return this.forced;
	}
}