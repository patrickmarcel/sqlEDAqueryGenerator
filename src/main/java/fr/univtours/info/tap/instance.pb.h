// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: instance.proto

#ifndef GOOGLE_PROTOBUF_INCLUDED_instance_2eproto
#define GOOGLE_PROTOBUF_INCLUDED_instance_2eproto

#include <limits>
#include <string>

#include <google/protobuf/port_def.inc>
#if PROTOBUF_VERSION < 3017000
#error This file was generated by a newer version of protoc which is
#error incompatible with your Protocol Buffer headers. Please update
#error your headers.
#endif
#if 3017003 < PROTOBUF_MIN_PROTOC_VERSION
#error This file was generated by an older version of protoc which is
#error incompatible with your Protocol Buffer headers. Please
#error regenerate this file with a newer version of protoc.
#endif

#include <google/protobuf/port_undef.inc>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/arena.h>
#include <google/protobuf/arenastring.h>
#include <google/protobuf/generated_message_table_driven.h>
#include <google/protobuf/generated_message_util.h>
#include <google/protobuf/metadata_lite.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/message.h>
#include <google/protobuf/repeated_field.h>  // IWYU pragma: export
#include <google/protobuf/extension_set.h>  // IWYU pragma: export
#include <google/protobuf/unknown_field_set.h>
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>
#define PROTOBUF_INTERNAL_EXPORT_instance_2eproto
PROTOBUF_NAMESPACE_OPEN
namespace internal {
class AnyMetadata;
}  // namespace internal
PROTOBUF_NAMESPACE_CLOSE

// Internal implementation detail -- do not use these members.
struct TableStruct_instance_2eproto {
  static const ::PROTOBUF_NAMESPACE_ID::internal::ParseTableField entries[]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::AuxiliaryParseTableField aux[]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::ParseTable schema[1]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::FieldMetadata field_metadata[];
  static const ::PROTOBUF_NAMESPACE_ID::internal::SerializationTable serialization_table[];
  static const ::PROTOBUF_NAMESPACE_ID::uint32 offsets[];
};
extern const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable descriptor_table_instance_2eproto;
class Instance;
struct InstanceDefaultTypeInternal;
extern InstanceDefaultTypeInternal _Instance_default_instance_;
PROTOBUF_NAMESPACE_OPEN
template<> ::Instance* Arena::CreateMaybeMessage<::Instance>(Arena*);
PROTOBUF_NAMESPACE_CLOSE

// ===================================================================

class Instance final :
    public ::PROTOBUF_NAMESPACE_ID::Message /* @@protoc_insertion_point(class_definition:Instance) */ {
 public:
  inline Instance() : Instance(nullptr) {}
  ~Instance() override;
  explicit constexpr Instance(::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized);

  Instance(const Instance& from);
  Instance(Instance&& from) noexcept
    : Instance() {
    *this = ::std::move(from);
  }

  inline Instance& operator=(const Instance& from) {
    CopyFrom(from);
    return *this;
  }
  inline Instance& operator=(Instance&& from) noexcept {
    if (this == &from) return *this;
    if (GetOwningArena() == from.GetOwningArena()) {
      InternalSwap(&from);
    } else {
      CopyFrom(from);
    }
    return *this;
  }

  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* descriptor() {
    return GetDescriptor();
  }
  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* GetDescriptor() {
    return default_instance().GetMetadata().descriptor;
  }
  static const ::PROTOBUF_NAMESPACE_ID::Reflection* GetReflection() {
    return default_instance().GetMetadata().reflection;
  }
  static const Instance& default_instance() {
    return *internal_default_instance();
  }
  static inline const Instance* internal_default_instance() {
    return reinterpret_cast<const Instance*>(
               &_Instance_default_instance_);
  }
  static constexpr int kIndexInFileMessages =
    0;

  friend void swap(Instance& a, Instance& b) {
    a.Swap(&b);
  }
  inline void Swap(Instance* other) {
    if (other == this) return;
    if (GetOwningArena() == other->GetOwningArena()) {
      InternalSwap(other);
    } else {
      ::PROTOBUF_NAMESPACE_ID::internal::GenericSwap(this, other);
    }
  }
  void UnsafeArenaSwap(Instance* other) {
    if (other == this) return;
    GOOGLE_DCHECK(GetOwningArena() == other->GetOwningArena());
    InternalSwap(other);
  }

  // implements Message ----------------------------------------------

  inline Instance* New() const final {
    return new Instance();
  }

  Instance* New(::PROTOBUF_NAMESPACE_ID::Arena* arena) const final {
    return CreateMaybeMessage<Instance>(arena);
  }
  using ::PROTOBUF_NAMESPACE_ID::Message::CopyFrom;
  void CopyFrom(const Instance& from);
  using ::PROTOBUF_NAMESPACE_ID::Message::MergeFrom;
  void MergeFrom(const Instance& from);
  private:
  static void MergeImpl(::PROTOBUF_NAMESPACE_ID::Message*to, const ::PROTOBUF_NAMESPACE_ID::Message&from);
  public:
  PROTOBUF_ATTRIBUTE_REINITIALIZES void Clear() final;
  bool IsInitialized() const final;

  size_t ByteSizeLong() const final;
  const char* _InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) final;
  ::PROTOBUF_NAMESPACE_ID::uint8* _InternalSerialize(
      ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const final;
  int GetCachedSize() const final { return _cached_size_.Get(); }

  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const final;
  void InternalSwap(Instance* other);
  friend class ::PROTOBUF_NAMESPACE_ID::internal::AnyMetadata;
  static ::PROTOBUF_NAMESPACE_ID::StringPiece FullMessageName() {
    return "Instance";
  }
  protected:
  explicit Instance(::PROTOBUF_NAMESPACE_ID::Arena* arena,
                       bool is_message_owned = false);
  private:
  static void ArenaDtor(void* object);
  inline void RegisterArenaDtor(::PROTOBUF_NAMESPACE_ID::Arena* arena);
  public:

  static const ClassData _class_data_;
  const ::PROTOBUF_NAMESPACE_ID::Message::ClassData*GetClassData() const final;

  ::PROTOBUF_NAMESPACE_ID::Metadata GetMetadata() const final;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  enum : int {
    kInterestsFieldNumber = 4,
    kCostsFieldNumber = 5,
    kDistancesFieldNumber = 6,
    kEpTimeFieldNumber = 2,
    kEpDistFieldNumber = 3,
    kSizeFieldNumber = 1,
  };
  // repeated double interests = 4 [packed = true];
  int interests_size() const;
  private:
  int _internal_interests_size() const;
  public:
  void clear_interests();
  private:
  double _internal_interests(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
      _internal_interests() const;
  void _internal_add_interests(double value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
      _internal_mutable_interests();
  public:
  double interests(int index) const;
  void set_interests(int index, double value);
  void add_interests(double value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
      interests() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
      mutable_interests();

  // repeated double costs = 5 [packed = true];
  int costs_size() const;
  private:
  int _internal_costs_size() const;
  public:
  void clear_costs();
  private:
  double _internal_costs(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
      _internal_costs() const;
  void _internal_add_costs(double value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
      _internal_mutable_costs();
  public:
  double costs(int index) const;
  void set_costs(int index, double value);
  void add_costs(double value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
      costs() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
      mutable_costs();

  // repeated double distances = 6 [packed = true];
  int distances_size() const;
  private:
  int _internal_distances_size() const;
  public:
  void clear_distances();
  private:
  double _internal_distances(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
      _internal_distances() const;
  void _internal_add_distances(double value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
      _internal_mutable_distances();
  public:
  double distances(int index) const;
  void set_distances(int index, double value);
  void add_distances(double value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
      distances() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
      mutable_distances();

  // double epTime = 2;
  void clear_eptime();
  double eptime() const;
  void set_eptime(double value);
  private:
  double _internal_eptime() const;
  void _internal_set_eptime(double value);
  public:

  // double epDist = 3;
  void clear_epdist();
  double epdist() const;
  void set_epdist(double value);
  private:
  double _internal_epdist() const;
  void _internal_set_epdist(double value);
  public:

  // uint32 size = 1;
  void clear_size();
  ::PROTOBUF_NAMESPACE_ID::uint32 size() const;
  void set_size(::PROTOBUF_NAMESPACE_ID::uint32 value);
  private:
  ::PROTOBUF_NAMESPACE_ID::uint32 _internal_size() const;
  void _internal_set_size(::PROTOBUF_NAMESPACE_ID::uint32 value);
  public:

  // @@protoc_insertion_point(class_scope:Instance)
 private:
  class _Internal;

  template <typename T> friend class ::PROTOBUF_NAMESPACE_ID::Arena::InternalHelper;
  typedef void InternalArenaConstructable_;
  typedef void DestructorSkippable_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double > interests_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double > costs_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< double > distances_;
  double eptime_;
  double epdist_;
  ::PROTOBUF_NAMESPACE_ID::uint32 size_;
  mutable ::PROTOBUF_NAMESPACE_ID::internal::CachedSize _cached_size_;
  friend struct ::TableStruct_instance_2eproto;
};
// ===================================================================


// ===================================================================

#ifdef __GNUC__
  #pragma GCC diagnostic push
  #pragma GCC diagnostic ignored "-Wstrict-aliasing"
#endif  // __GNUC__
// Instance

// uint32 size = 1;
inline void Instance::clear_size() {
  size_ = 0u;
}
inline ::PROTOBUF_NAMESPACE_ID::uint32 Instance::_internal_size() const {
  return size_;
}
inline ::PROTOBUF_NAMESPACE_ID::uint32 Instance::size() const {
  // @@protoc_insertion_point(field_get:Instance.size)
  return _internal_size();
}
inline void Instance::_internal_set_size(::PROTOBUF_NAMESPACE_ID::uint32 value) {
  
  size_ = value;
}
inline void Instance::set_size(::PROTOBUF_NAMESPACE_ID::uint32 value) {
  _internal_set_size(value);
  // @@protoc_insertion_point(field_set:Instance.size)
}

// double epTime = 2;
inline void Instance::clear_eptime() {
  eptime_ = 0;
}
inline double Instance::_internal_eptime() const {
  return eptime_;
}
inline double Instance::eptime() const {
  // @@protoc_insertion_point(field_get:Instance.epTime)
  return _internal_eptime();
}
inline void Instance::_internal_set_eptime(double value) {
  
  eptime_ = value;
}
inline void Instance::set_eptime(double value) {
  _internal_set_eptime(value);
  // @@protoc_insertion_point(field_set:Instance.epTime)
}

// double epDist = 3;
inline void Instance::clear_epdist() {
  epdist_ = 0;
}
inline double Instance::_internal_epdist() const {
  return epdist_;
}
inline double Instance::epdist() const {
  // @@protoc_insertion_point(field_get:Instance.epDist)
  return _internal_epdist();
}
inline void Instance::_internal_set_epdist(double value) {
  
  epdist_ = value;
}
inline void Instance::set_epdist(double value) {
  _internal_set_epdist(value);
  // @@protoc_insertion_point(field_set:Instance.epDist)
}

// repeated double interests = 4 [packed = true];
inline int Instance::_internal_interests_size() const {
  return interests_.size();
}
inline int Instance::interests_size() const {
  return _internal_interests_size();
}
inline void Instance::clear_interests() {
  interests_.Clear();
}
inline double Instance::_internal_interests(int index) const {
  return interests_.Get(index);
}
inline double Instance::interests(int index) const {
  // @@protoc_insertion_point(field_get:Instance.interests)
  return _internal_interests(index);
}
inline void Instance::set_interests(int index, double value) {
  interests_.Set(index, value);
  // @@protoc_insertion_point(field_set:Instance.interests)
}
inline void Instance::_internal_add_interests(double value) {
  interests_.Add(value);
}
inline void Instance::add_interests(double value) {
  _internal_add_interests(value);
  // @@protoc_insertion_point(field_add:Instance.interests)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
Instance::_internal_interests() const {
  return interests_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
Instance::interests() const {
  // @@protoc_insertion_point(field_list:Instance.interests)
  return _internal_interests();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
Instance::_internal_mutable_interests() {
  return &interests_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
Instance::mutable_interests() {
  // @@protoc_insertion_point(field_mutable_list:Instance.interests)
  return _internal_mutable_interests();
}

// repeated double costs = 5 [packed = true];
inline int Instance::_internal_costs_size() const {
  return costs_.size();
}
inline int Instance::costs_size() const {
  return _internal_costs_size();
}
inline void Instance::clear_costs() {
  costs_.Clear();
}
inline double Instance::_internal_costs(int index) const {
  return costs_.Get(index);
}
inline double Instance::costs(int index) const {
  // @@protoc_insertion_point(field_get:Instance.costs)
  return _internal_costs(index);
}
inline void Instance::set_costs(int index, double value) {
  costs_.Set(index, value);
  // @@protoc_insertion_point(field_set:Instance.costs)
}
inline void Instance::_internal_add_costs(double value) {
  costs_.Add(value);
}
inline void Instance::add_costs(double value) {
  _internal_add_costs(value);
  // @@protoc_insertion_point(field_add:Instance.costs)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
Instance::_internal_costs() const {
  return costs_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
Instance::costs() const {
  // @@protoc_insertion_point(field_list:Instance.costs)
  return _internal_costs();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
Instance::_internal_mutable_costs() {
  return &costs_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
Instance::mutable_costs() {
  // @@protoc_insertion_point(field_mutable_list:Instance.costs)
  return _internal_mutable_costs();
}

// repeated double distances = 6 [packed = true];
inline int Instance::_internal_distances_size() const {
  return distances_.size();
}
inline int Instance::distances_size() const {
  return _internal_distances_size();
}
inline void Instance::clear_distances() {
  distances_.Clear();
}
inline double Instance::_internal_distances(int index) const {
  return distances_.Get(index);
}
inline double Instance::distances(int index) const {
  // @@protoc_insertion_point(field_get:Instance.distances)
  return _internal_distances(index);
}
inline void Instance::set_distances(int index, double value) {
  distances_.Set(index, value);
  // @@protoc_insertion_point(field_set:Instance.distances)
}
inline void Instance::_internal_add_distances(double value) {
  distances_.Add(value);
}
inline void Instance::add_distances(double value) {
  _internal_add_distances(value);
  // @@protoc_insertion_point(field_add:Instance.distances)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
Instance::_internal_distances() const {
  return distances_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >&
Instance::distances() const {
  // @@protoc_insertion_point(field_list:Instance.distances)
  return _internal_distances();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
Instance::_internal_mutable_distances() {
  return &distances_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< double >*
Instance::mutable_distances() {
  // @@protoc_insertion_point(field_mutable_list:Instance.distances)
  return _internal_mutable_distances();
}

#ifdef __GNUC__
  #pragma GCC diagnostic pop
#endif  // __GNUC__

// @@protoc_insertion_point(namespace_scope)


// @@protoc_insertion_point(global_scope)

#include <google/protobuf/port_undef.inc>
#endif  // GOOGLE_PROTOBUF_INCLUDED_GOOGLE_PROTOBUF_INCLUDED_instance_2eproto
