package com.surialabs.rn.braintree;

import android.content.Intent;
import android.content.Context;
import android.app.Activity;

import com.braintreepayments.api.PaymentRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ActivityEventListener;

public class Braintree extends ReactContextBaseJavaModule implements ActivityEventListener {
  private static final int PAYMENT_REQUEST = 1;
  private String token;

  private Callback successCallback;
  private Callback errorCallback;

  private Context mActivityContext;

  private BraintreeFragment mBraintreeFragment;

  public Braintree(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addActivityEventListener(this);
  }

  @Override
  public String getName() {
    return "Braintree";
  }

  public String getToken() {
    return this.token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  @ReactMethod
  public void setup(final String token, final Callback successCallback, final Callback errorCallback) {
    try {
      this.mBraintreeFragment = BraintreeFragment.newInstance(getCurrentActivity(), token);
      this.mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
        @Override
        public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
          nonceCallback(paymentMethodNonce.getNonce());
        }
      });
      this.setToken(token);
      successCallback.invoke(this.getToken());
    } catch (InvalidArgumentException e) {
      errorCallback.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void getCardNonce(final String cardNumber, final String expirationMonth, final String expirationYear, final Callback successCallback, final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    CardBuilder cardBuilder = new CardBuilder()
      .cardNumber(cardNumber)
      .expirationMonth(expirationMonth)
      .expirationYear(expirationYear);

    Card.tokenize(this.mBraintreeFragment, cardBuilder);
  }

  public void nonceCallback(String nonce) {
    this.successCallback.invoke(nonce);
  }

  @ReactMethod
  public void paymentRequest(final Callback successCallback, final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    PaymentRequest paymentRequest = new PaymentRequest()
    .clientToken(this.getToken());

    (getCurrentActivity()).startActivityForResult(
      paymentRequest.getIntent(getCurrentActivity()),
      PAYMENT_REQUEST
    );
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode == PAYMENT_REQUEST) {
      switch (resultCode) {
        case Activity.RESULT_OK:
          PaymentMethodNonce paymentMethodNonce = data.getParcelableExtra(
            BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE
          );
          this.successCallback.invoke(paymentMethodNonce.getNonce());
          break;
        case BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR:
        case BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR:
        case BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_UNAVAILABLE:
          this.errorCallback.invoke(
            data.getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE)
          );
          break;
        default:
          break;
      }
    }
  }

  @Override
  public void onNewIntent(Intent intent){
    
  }
}
