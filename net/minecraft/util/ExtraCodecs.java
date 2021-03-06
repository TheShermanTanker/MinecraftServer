package net.minecraft.util;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.SystemUtils;
import org.apache.commons.lang3.mutable.MutableObject;

public class ExtraCodecs {
    public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, (v) -> {
        return "Value must be non-negative: " + v;
    });
    public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, (v) -> {
        return "Value must be positive: " + v;
    });
    public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, (v) -> {
        return "Value must be positive: " + v;
    });

    public static <F, S> Codec<Either<F, S>> xor(Codec<F> first, Codec<S> second) {
        return new ExtraCodecs.XorCodec<>(first, second);
    }

    public static <P, I> Codec<I> intervalCodec(Codec<P> codec, String leftFieldName, String rightFieldName, BiFunction<P, P, DataResult<I>> combineFunction, Function<I, P> leftFunction, Function<I, P> rightFunction) {
        Codec<I> codec2 = Codec.list(codec).comapFlatMap((list) -> {
            return SystemUtils.fixedSize(list, 2).flatMap((listx) -> {
                P object = listx.get(0);
                P object2 = listx.get(1);
                return combineFunction.apply(object, object2);
            });
        }, (object) -> {
            return ImmutableList.of(leftFunction.apply(object), rightFunction.apply(object));
        });
        Codec<I> codec3 = RecordCodecBuilder.create((instance) -> {
            return instance.group(codec.fieldOf(leftFieldName).forGetter(Pair::getFirst), codec.fieldOf(rightFieldName).forGetter(Pair::getSecond)).apply(instance, Pair::of);
        }).comapFlatMap((pair) -> {
            return combineFunction.apply((P)pair.getFirst(), (P)pair.getSecond());
        }, (object) -> {
            return Pair.of(leftFunction.apply(object), rightFunction.apply(object));
        });
        Codec<I> codec4 = (new ExtraCodecs.EitherCodec<>(codec2, codec3)).xmap((either) -> {
            return either.map((object) -> {
                return object;
            }, (object) -> {
                return object;
            });
        }, Either::left);
        return Codec.either(codec, codec4).comapFlatMap((either) -> {
            return either.map((object) -> {
                return combineFunction.apply(object, object);
            }, DataResult::success);
        }, (object) -> {
            P object2 = leftFunction.apply(object);
            P object3 = rightFunction.apply(object);
            return Objects.equals(object2, object3) ? Either.left(object2) : Either.right(object);
        });
    }

    public static <A> ResultFunction<A> orElsePartial(A object) {
        return new ResultFunction<A>() {
            public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> dynamicOps, T object, DataResult<Pair<A, T>> dataResult) {
                MutableObject<String> mutableObject = new MutableObject<>();
                Optional<Pair<A, T>> optional = dataResult.resultOrPartial(mutableObject::setValue);
                return optional.isPresent() ? dataResult : DataResult.error("(" + (String)mutableObject.getValue() + " -> using default)", Pair.of(object, object));
            }

            public <T> DataResult<T> coApply(DynamicOps<T> dynamicOps, A object, DataResult<T> dataResult) {
                return dataResult;
            }

            @Override
            public String toString() {
                return "OrElsePartial[" + object + "]";
            }
        };
    }

    public static <E> Codec<E> idResolverCodec(ToIntFunction<E> toIntFunction, IntFunction<E> intFunction, int i) {
        return Codec.INT.flatXmap((integer) -> {
            return Optional.ofNullable(intFunction.apply(integer)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown element id: " + integer);
            });
        }, (object) -> {
            int j = toIntFunction.applyAsInt(object);
            return j == i ? DataResult.error("Element with unknown id: " + object) : DataResult.success(j);
        });
    }

    public static <E> Codec<E> stringResolverCodec(Function<E, String> function, Function<String, E> function2) {
        return Codec.STRING.flatXmap((string) -> {
            return Optional.ofNullable(function2.apply(string)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown element name:" + string);
            });
        }, (object) -> {
            return Optional.ofNullable(function.apply(object)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Element with unknown name: " + object);
            });
        });
    }

    public static <E> Codec<E> orCompressed(Codec<E> codec, Codec<E> codec2) {
        return new Codec<E>() {
            public <T> DataResult<T> encode(E object, DynamicOps<T> dynamicOps, T object2) {
                return dynamicOps.compressMaps() ? codec2.encode(object, dynamicOps, object2) : codec.encode(object, dynamicOps, object2);
            }

            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> dynamicOps, T object) {
                return dynamicOps.compressMaps() ? codec2.decode(dynamicOps, object) : codec.decode(dynamicOps, object);
            }

            @Override
            public String toString() {
                return codec + " orCompressed " + codec2;
            }
        };
    }

    public static <E> Codec<E> overrideLifecycle(Codec<E> codec, Function<E, Lifecycle> function, Function<E, Lifecycle> function2) {
        return codec.mapResult(new ResultFunction<E>() {
            public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> dynamicOps, T object, DataResult<Pair<E, T>> dataResult) {
                return dataResult.result().map((pair) -> {
                    return dataResult.setLifecycle(function.apply(pair.getFirst()));
                }).orElse(dataResult);
            }

            public <T> DataResult<T> coApply(DynamicOps<T> dynamicOps, E object, DataResult<T> dataResult) {
                return dataResult.setLifecycle(function2.apply(object));
            }

            @Override
            public String toString() {
                return "WithLifecycle[" + function + " " + function2 + "]";
            }
        });
    }

    private static <N extends Number & Comparable<N>> Function<N, DataResult<N>> checkRangeWithMessage(N min, N max, Function<N, String> messageFactory) {
        return (value) -> {
            return value.compareTo(min) >= 0 && value.compareTo(max) <= 0 ? DataResult.success(value) : DataResult.error(messageFactory.apply(value));
        };
    }

    private static Codec<Integer> intRangeWithMessage(int min, int max, Function<Integer, String> messageFactory) {
        Function<Integer, DataResult<Integer>> function = checkRangeWithMessage(min, max, messageFactory);
        return Codec.INT.flatXmap(function, function);
    }

    private static <N extends Number & Comparable<N>> Function<N, DataResult<N>> checkRangeMinExclusiveWithMessage(N min, N max, Function<N, String> messageFactory) {
        return (value) -> {
            return value.compareTo(min) > 0 && value.compareTo(max) <= 0 ? DataResult.success(value) : DataResult.error(messageFactory.apply(value));
        };
    }

    private static Codec<Float> floatRangeMinExclusiveWithMessage(float min, float max, Function<Float, String> messageFactory) {
        Function<Float, DataResult<Float>> function = checkRangeMinExclusiveWithMessage(min, max, messageFactory);
        return Codec.FLOAT.flatXmap(function, function);
    }

    public static <T> Function<List<T>, DataResult<List<T>>> nonEmptyListCheck() {
        return (list) -> {
            return list.isEmpty() ? DataResult.error("List must have contents") : DataResult.success(list);
        };
    }

    public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> originalCodec) {
        return originalCodec.flatXmap(nonEmptyListCheck(), nonEmptyListCheck());
    }

    public static <T> Function<List<Supplier<T>>, DataResult<List<Supplier<T>>>> nonNullSupplierListCheck() {
        return (suppliers) -> {
            List<String> list = Lists.newArrayList();

            for(int i = 0; i < suppliers.size(); ++i) {
                Supplier<T> supplier = suppliers.get(i);

                try {
                    if (supplier.get() == null) {
                        list.add("Missing value [" + i + "] : " + supplier);
                    }
                } catch (Exception var5) {
                    list.add("Invalid value [" + i + "]: " + supplier + ", message: " + var5.getMessage());
                }
            }

            return !list.isEmpty() ? DataResult.error(String.join("; ", list)) : DataResult.success(suppliers, Lifecycle.stable());
        };
    }

    public static <T> Function<Supplier<T>, DataResult<Supplier<T>>> nonNullSupplierCheck() {
        return (supplier) -> {
            try {
                if (supplier.get() == null) {
                    return DataResult.error("Missing value: " + supplier);
                }
            } catch (Exception var2) {
                return DataResult.error("Invalid value: " + supplier + ", message: " + var2.getMessage());
            }

            return DataResult.success(supplier, Lifecycle.stable());
        };
    }

    public static <A> Codec<A> lazyInitializedCodec(Supplier<Codec<A>> supplier) {
        return new ExtraCodecs.LazyInitializedCodec<>(supplier);
    }

    static final class EitherCodec<F, S> implements Codec<Either<F, S>> {
        private final Codec<F> first;
        private final Codec<S> second;

        public EitherCodec(Codec<F> first, Codec<S> second) {
            this.first = first;
            this.second = second;
        }

        public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> dynamicOps, T object) {
            DataResult<Pair<Either<F, S>, T>> dataResult = this.first.decode(dynamicOps, object).map((pair) -> {
                return pair.mapFirst(Either::left);
            });
            if (!dataResult.error().isPresent()) {
                return dataResult;
            } else {
                DataResult<Pair<Either<F, S>, T>> dataResult2 = this.second.decode(dynamicOps, object).map((pair) -> {
                    return pair.mapFirst(Either::right);
                });
                return !dataResult2.error().isPresent() ? dataResult2 : dataResult.apply2((pair, pair2) -> {
                    return pair2;
                }, dataResult2);
            }
        }

        public <T> DataResult<T> encode(Either<F, S> either, DynamicOps<T> dynamicOps, T object) {
            return either.map((left) -> {
                return this.first.encode(left, dynamicOps, object);
            }, (right) -> {
                return this.second.encode(right, dynamicOps, object);
            });
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                ExtraCodecs.EitherCodec<?, ?> eitherCodec = (ExtraCodecs.EitherCodec)object;
                return Objects.equals(this.first, eitherCodec.first) && Objects.equals(this.second, eitherCodec.second);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.first, this.second);
        }

        @Override
        public String toString() {
            return "EitherCodec[" + this.first + ", " + this.second + "]";
        }
    }

    static record LazyInitializedCodec<A>(Supplier<Codec<A>> delegate) implements Codec<A> {
        LazyInitializedCodec(Supplier<Codec<A>> supplier) {
            supplier = Suppliers.memoize(supplier::get);
            this.delegate = supplier;
        }

        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> dynamicOps, T object) {
            return this.delegate.get().decode(dynamicOps, object);
        }

        public <T> DataResult<T> encode(A object, DynamicOps<T> dynamicOps, T object2) {
            return this.delegate.get().encode(object, dynamicOps, object2);
        }

        public Supplier<Codec<A>> delegate() {
            return this.delegate;
        }
    }

    static final class XorCodec<F, S> implements Codec<Either<F, S>> {
        private final Codec<F> first;
        private final Codec<S> second;

        public XorCodec(Codec<F> first, Codec<S> second) {
            this.first = first;
            this.second = second;
        }

        public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> dynamicOps, T object) {
            DataResult<Pair<Either<F, S>, T>> dataResult = this.first.decode(dynamicOps, object).map((pair) -> {
                return pair.mapFirst(Either::left);
            });
            DataResult<Pair<Either<F, S>, T>> dataResult2 = this.second.decode(dynamicOps, object).map((pair) -> {
                return pair.mapFirst(Either::right);
            });
            Optional<Pair<Either<F, S>, T>> optional = dataResult.result();
            Optional<Pair<Either<F, S>, T>> optional2 = dataResult2.result();
            if (optional.isPresent() && optional2.isPresent()) {
                return DataResult.error("Both alternatives read successfully, can not pick the correct one; first: " + optional.get() + " second: " + optional2.get(), optional.get());
            } else {
                return optional.isPresent() ? dataResult : dataResult2;
            }
        }

        public <T> DataResult<T> encode(Either<F, S> either, DynamicOps<T> dynamicOps, T object) {
            return either.map((left) -> {
                return this.first.encode(left, dynamicOps, object);
            }, (right) -> {
                return this.second.encode(right, dynamicOps, object);
            });
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                ExtraCodecs.XorCodec<?, ?> xorCodec = (ExtraCodecs.XorCodec)object;
                return Objects.equals(this.first, xorCodec.first) && Objects.equals(this.second, xorCodec.second);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.first, this.second);
        }

        @Override
        public String toString() {
            return "XorCodec[" + this.first + ", " + this.second + "]";
        }
    }
}
